package ru.ytkab0bp.slicebeam.slic3r;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ytkab0bp.slicebeam.config.ConfigObject;
import ru.ytkab0bp.slicebeam.view.GLView;

public final class GCodeThumbnailer {
    private static final String TAG = "GCodeThumbnailer";
    private static final int MAX_ROW_LENGTH = 78;
    private static final int THUMBNAIL_SUPERSAMPLE = 16;
    private static final int MAX_SUPERSAMPLE_PIXELS = 16_000_000;
    private static final int MAX_SUPERSAMPLE_DIM = 4096;
    private static final Pattern SIZE_PATTERN = Pattern.compile("^(\\d+)\\s*[xX]\\s*(\\d+)$");

    private GCodeThumbnailer() {}

    public static boolean addThumbnailsToGcode(File gcodeFile, ConfigObject config, GLView glView) {
        if (gcodeFile == null || config == null || glView == null) {
            return false;
        }
        if (!gcodeFile.exists()) {
            return false;
        }
        String binaryGcode = config.get("binary_gcode");
        if ("1".equals(binaryGcode)) {
            return false;
        }
        if (gcodeHasThumbnail(gcodeFile)) {
            return false;
        }
        String thumbnails = config.get("thumbnails");
        if (thumbnails == null || thumbnails.trim().isEmpty()) {
            return false;
        }
        String defaultFormat = "PNG";

        List<ThumbnailSpec> specs = parseThumbnailSpecs(thumbnails, defaultFormat);
        if (specs.isEmpty()) {
            return false;
        }

        String header = buildHeader(specs, glView);
        if (header.isEmpty()) {
            return false;
        }

        try {
            return prependToFile(gcodeFile, header);
        } catch (IOException e) {
            Log.e(TAG, "Failed to add thumbnails to gcode", e);
            return false;
        }
    }

    private static List<ThumbnailSpec> parseThumbnailSpecs(String thumbnails, String defaultFormat) {
        List<ThumbnailSpec> specs = new ArrayList<>();
        for (String raw : thumbnails.split(",")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.contains("COLPIC")) {
                continue;
            }
            String format = defaultFormat;
            int slash = token.indexOf('/');
            if (slash >= 0) {
                format = token.substring(slash + 1).trim();
                token = token.substring(0, slash).trim();
            }
            Matcher matcher = SIZE_PATTERN.matcher(token);
            if (!matcher.matches()) {
                continue;
            }
            int width = Integer.parseInt(matcher.group(1));
            int height = Integer.parseInt(matcher.group(2));
            if (width <= 0 || height <= 0) {
                continue;
            }
            specs.add(new ThumbnailSpec(width, height, normalizeFormat(format)));
        }
        return specs;
    }

    private static ThumbnailFormat normalizeFormat(String format) {
        if (format == null) {
            return ThumbnailFormat.PNG;
        }
        switch (format.trim().toUpperCase(Locale.US)) {
            case "JPG":
            case "JPEG":
                return ThumbnailFormat.JPG;
            case "QOI":
                return ThumbnailFormat.QOI;
            default:
                return ThumbnailFormat.PNG;
        }
    }

    private static Bitmap captureSnapshot(GLView glView, int targetWidth, int targetHeight) {
        CountDownLatch latch = new CountDownLatch(1);
        Bitmap[] ref = new Bitmap[1];
        int scale = computeSupersampleScale(targetWidth, targetHeight);
        int width = targetWidth * scale;
        int height = targetHeight * scale;
        glView.queueEvent(() -> {
            try {
                ref[0] = glView.snapshotBitmap(width, height, true, true);
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Thumbnail snapshot OOM", e);
            } catch (Exception e) {
                Log.e(TAG, "Failed to capture GL snapshot", e);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                Log.w(TAG, "Timed out waiting for GL snapshot");
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        Bitmap snapshot = ref[0];
        if (snapshot == null) {
            return null;
        }
        if (scale <= 1) {
            return snapshot;
        }
        Bitmap downscaled = downscaleBitmap(snapshot, targetWidth, targetHeight);
        if (downscaled != snapshot) {
            snapshot.recycle();
        }
        return downscaled;
    }

    private static int computeSupersampleScale(int w, int h) {
        if (w <= 0 || h <= 0) {
            return 1;
        }
        int scale = THUMBNAIL_SUPERSAMPLE;
        while (scale > 1) {
            long pixels = (long) w * h * scale * scale;
            if (pixels <= MAX_SUPERSAMPLE_PIXELS && w * scale <= MAX_SUPERSAMPLE_DIM && h * scale <= MAX_SUPERSAMPLE_DIM) {
                break;
            }
            scale--;
        }
        return Math.max(1, scale);
    }

    private static String buildHeader(List<ThumbnailSpec> specs, GLView glView) {
        StringBuilder header = new StringBuilder();
        boolean wroteThumbnail = false;
        for (ThumbnailSpec spec : specs) {
            Bitmap snapshot = captureSnapshot(glView, spec.width, spec.height);
            if (snapshot == null) {
                continue;
            }
            ThumbnailFormat format = spec.format;
            if (format == ThumbnailFormat.QOI) {
                Log.w(TAG, "QOI thumbnails not supported, falling back to PNG");
                format = ThumbnailFormat.PNG;
            }
            Bitmap scaled = snapshot;
            if (snapshot.getWidth() != spec.width || snapshot.getHeight() != spec.height) {
                scaled = downscaleBitmap(snapshot, spec.width, spec.height);
                snapshot.recycle();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Bitmap.CompressFormat compressFormat = format == ThumbnailFormat.JPG
                    ? Bitmap.CompressFormat.JPEG
                    : Bitmap.CompressFormat.PNG;
            int quality = format == ThumbnailFormat.JPG ? 90 : 100;
            if (!scaled.compress(compressFormat, quality, out)) {
                scaled.recycle();
                continue;
            }
            scaled.recycle();
            byte[] data = out.toByteArray();
            if (data.length == 0) {
                continue;
            }
            String tag = format == ThumbnailFormat.JPG ? "thumbnail_JPG" : "thumbnail";
            if (!wroteThumbnail) {
                header.append("; THUMBNAIL_BLOCK_START\n");
                wroteThumbnail = true;
            }
            String encoded = Base64.encodeToString(data, Base64.NO_WRAP);
            header.append("\n;\n; ").append(tag)
                    .append(" begin ").append(spec.width).append("x").append(spec.height)
                    .append(" ").append(encoded.length()).append("\n");
            int offset = 0;
            while (offset < encoded.length()) {
                int end = Math.min(offset + MAX_ROW_LENGTH, encoded.length());
                header.append("; ").append(encoded, offset, end).append("\n");
                offset = end;
            }
            header.append("; ").append(tag).append(" end\n;\n");
        }
        if (wroteThumbnail) {
            header.append("; THUMBNAIL_BLOCK_END\n");
        }
        return header.toString();
    }

    private static Bitmap downscaleBitmap(Bitmap src, int targetWidth, int targetHeight) {
        if (src.getWidth() == targetWidth && src.getHeight() == targetHeight) {
            return src;
        }
        Bitmap current = src;
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        while (current.getWidth() / 2 >= targetWidth && current.getHeight() / 2 >= targetHeight) {
            int nextWidth = Math.max(targetWidth, current.getWidth() / 2);
            int nextHeight = Math.max(targetHeight, current.getHeight() / 2);
            Bitmap next = Bitmap.createBitmap(nextWidth, nextHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(next);
            canvas.drawBitmap(current, null, new Rect(0, 0, nextWidth, nextHeight), paint);
            if (current != src) {
                current.recycle();
            }
            current = next;
        }
        if (current.getWidth() != targetWidth || current.getHeight() != targetHeight) {
            Bitmap finalBmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBmp);
            canvas.drawBitmap(current, null, new Rect(0, 0, targetWidth, targetHeight), paint);
            if (current != src) {
                current.recycle();
            }
            current = finalBmp;
        }
        return current;
    }

    private static boolean prependToFile(File gcodeFile, String header) throws IOException {
        File tmp = new File(gcodeFile.getParentFile(), gcodeFile.getName() + ".thumbtmp");
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(gcodeFile));
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
            out.write(header.getBytes(StandardCharsets.US_ASCII));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        if (!gcodeFile.delete()) {
            tmp.delete();
            return false;
        }
        if (!tmp.renameTo(gcodeFile)) {
            tmp.delete();
            return false;
        }
        return true;
    }

    private static boolean gcodeHasThumbnail(File gcodeFile) {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(gcodeFile))) {
            byte[] buffer = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1 && total < 262144) {
                sb.append(new String(buffer, 0, read, StandardCharsets.US_ASCII));
                if (sb.indexOf("thumbnail begin") != -1 || sb.indexOf("thumbnail_JPG begin") != -1) {
                    return true;
                }
                total += read;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static final class ThumbnailSpec {
        final int width;
        final int height;
        final ThumbnailFormat format;

        ThumbnailSpec(int width, int height, ThumbnailFormat format) {
            this.width = width;
            this.height = height;
            this.format = format;
        }
    }

    private enum ThumbnailFormat {
        PNG,
        JPG,
        QOI
    }
}
