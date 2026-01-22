package ru.ytkab0bp.slicebeam.print_host;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

public final class ElegooLinkClient {
    private static final int MAX_UPLOAD_PACKAGE_LENGTH = 1024 * 1024;
    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

    private ElegooLinkClient() {}

    public static Result upload(File gcode, String host, String uploadName, boolean startPrint, boolean timelapse, boolean bedLeveling, int bedType) {
        if (gcode == null || !gcode.exists()) {
            return Result.error("G-code file not found.");
        }
        String finalName = (uploadName == null || uploadName.isEmpty()) ? gcode.getName() : uploadName;
        String baseUrl = normalizeBaseUrl(host);
        String uploadUrl = baseUrl + "/uploadFile/upload";
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(60, TimeUnit.SECONDS)
                .build();

        String md5;
        try {
            md5 = md5File(gcode);
        } catch (Exception e) {
            return Result.error("Failed to compute MD5: " + e.getMessage());
        }
        long size = gcode.length();
        String uuid = UUID.randomUUID().toString().replace("-", "");

        int packageCount = (int) ((size + MAX_UPLOAD_PACKAGE_LENGTH - 1) / MAX_UPLOAD_PACKAGE_LENGTH);
        for (int i = 0; i < packageCount; i++) {
            long offset = (long) MAX_UPLOAD_PACKAGE_LENGTH * i;
            long length = Math.min(MAX_UPLOAD_PACKAGE_LENGTH, size - offset);
            Result partRes = uploadPart(client, uploadUrl, gcode, finalName, md5, uuid, size, offset, length);
            if (!partRes.ok) {
                return partRes;
            }
        }

        if (!startPrint) {
            return Result.ok();
        }
        return startPrint(client, host, finalName, timelapse, bedLeveling, bedType);
    }

    private static Result uploadPart(OkHttpClient client, String url, File file, String uploadName, String md5, String uuid, long totalSize, long offset, long length) {
        RequestBody fileBody = new FileSliceRequestBody(file, offset, length);
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("Check", "1")
                .addFormDataPart("S-File-MD5", md5)
                .addFormDataPart("Offset", String.valueOf(offset))
                .addFormDataPart("Uuid", uuid)
                .addFormDataPart("TotalSize", String.valueOf(totalSize))
                .addFormDataPart("File", uploadName, fileBody)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                return Result.error("Upload failed: HTTP " + response.code());
            }
            if (!isElegooOk(body)) {
                return Result.error(parseElegooError(body));
            }
            return Result.ok();
        } catch (IOException e) {
            return Result.error("Upload failed: " + e.getMessage());
        }
    }

    private static Result startPrint(OkHttpClient client, String host, String filename, boolean timelapse, boolean bedLeveling, int bedType) {
        WebSocketSession session = null;
        String lastError = null;
        for (String wsUrl : buildWebSocketCandidates(host)) {
            WebSocketSession attempt = new WebSocketSession(client, wsUrl);
            if (attempt.awaitOpen(10, TimeUnit.SECONDS)) {
                session = attempt;
                break;
            }
            lastError = attempt.failureSummary();
            if (attempt.isTooManyClients()) {
                return Result.error("Printer reports too many connected clients. Close the ElegooLink web UI and other apps, then try again.");
            }
            attempt.close();
        }
        if (session == null) {
            if (lastError != null && !lastError.isEmpty()) {
                return Result.error("Failed to connect to ElegooLink websocket. " + lastError);
            }
            return Result.error("Failed to connect to ElegooLink websocket.");
        }

        String requestId = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        String json = "{"
                + "\"Id\":\"\","
                + "\"Data\":{"
                + "\"Cmd\":128,"
                + "\"Data\":{"
                + "\"Filename\":\"/local/" + filename + "\","
                + "\"StartLayer\":0,"
                + "\"Calibration_switch\":" + (bedLeveling ? 1 : 0) + ","
                + "\"PrintPlatformType\":" + (bedType != 0 ? 1 : 0) + ","
                + "\"Tlp_Switch\":" + (timelapse ? 1 : 0)
                + "},"
                + "\"RequestID\":\"" + requestId + "\","
                + "\"MainboardID\":\"\","
                + "\"TimeStamp\":" + timestamp + ","
                + "\"From\":1"
                + "}"
                + "}";

        session.sendText(json);
        String response = session.receiveText(30, TimeUnit.SECONDS);
        if (response == null) {
            session.close();
            return Result.error("Start print timeout.");
        }
        try {
            JSONObject root = new JSONObject(response);
            JSONObject data = root.optJSONObject("Data");
            if (data == null) {
                session.close();
                return Result.error("Invalid response from printer.");
            }
            int cmd = data.optInt("Cmd", -1);
            if (cmd != 128) {
                session.close();
                return Result.error("Unexpected response from printer.");
            }
            JSONObject ackData = data.optJSONObject("Data");
            int ack = ackData != null ? ackData.optInt("Ack", -1) : -1;
            if (ack == 0) {
                session.close();
                return Result.ok();
            }
            String error = mapAckError(ack);
            session.close();
            return Result.error(error);
        } catch (JSONException e) {
            session.close();
            return Result.error("Invalid response from printer.");
        }
    }

    private static boolean isElegooOk(String body) {
        try {
            JSONObject root = new JSONObject(body);
            String code = root.optString("code", "");
            return "000000".equals(code);
        } catch (JSONException e) {
            return false;
        }
    }

    private static String parseElegooError(String body) {
        try {
            JSONObject root = new JSONObject(body);
            String code = root.optString("code", "unknown");
            StringBuilder sb = new StringBuilder();
            sb.append("ErrorCode: ").append(code);
            JSONArray messages = root.optJSONArray("messages");
            if (messages != null) {
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject msg = messages.optJSONObject(i);
                    if (msg != null) {
                        sb.append("\n").append(msg.optString("field", ""))
                                .append(":").append(msg.optString("message", ""));
                    }
                }
            }
            return sb.toString();
        } catch (JSONException e) {
            return "Upload failed.";
        }
    }

    private static String mapAckError(int ack) {
        switch (ack) {
            case 1:
                return "The printer is busy.";
            case 2:
                return "The file is missing.";
            case 3:
                return "MD5 check failed.";
            case 4:
                return "File I/O error.";
            case 5:
            case 6:
                return "File format or resolution is invalid.";
            case 7:
                return "File does not match the printer.";
            default:
                return "Unknown error. Error code: " + ack;
        }
    }

    private static String normalizeBaseUrl(String host) {
        String value = host.trim();
        if (!value.contains("://")) {
            value = "http://" + value;
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static List<String> buildWebSocketCandidates(String host) {
        String h = sanitizeWebSocketHost(host);
        List<String> urls = new ArrayList<>(1);
        urls.add(String.format(Locale.US, "ws://%s:3030/websocket", h));
        return urls;
    }

    private static String sanitizeWebSocketHost(String host) {
        String base = normalizeBaseUrl(host);
        try {
            URI uri = new URI(base);
            String h = uri.getHost() != null ? uri.getHost() : uri.getAuthority();
            if (h != null && h.contains(":")) {
                h = h.substring(0, h.indexOf(':'));
            }
            if (h == null || h.isEmpty()) {
                h = host;
            }
            return wrapIpv6Host(h);
        } catch (URISyntaxException e) {
            String h = host;
            int schemeIdx = h.indexOf("://");
            if (schemeIdx != -1) {
                h = h.substring(schemeIdx + 3);
            }
            int slashIdx = h.indexOf('/');
            if (slashIdx != -1) {
                h = h.substring(0, slashIdx);
            }
            int portIdx = h.indexOf(':');
            if (portIdx != -1) {
                h = h.substring(0, portIdx);
            }
            return wrapIpv6Host(h);
        }
    }

    private static String wrapIpv6Host(String host) {
        if (host == null || host.isEmpty()) {
            return host;
        }
        if (host.indexOf(':') != -1 && !host.startsWith("[") && !host.endsWith("]")) {
            return "[" + host + "]";
        }
        return host;
    }

    private static String md5File(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString();
    }

    private static final class FileSliceRequestBody extends RequestBody {
        private final File file;
        private final long offset;
        private final long length;

        FileSliceRequestBody(File file, long offset, long length) {
            this.file = file;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public MediaType contentType() {
            return OCTET_STREAM;
        }

        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                raf.seek(offset);
                byte[] buffer = new byte[8192];
                long remaining = length;
                while (remaining > 0) {
                    int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read == -1) {
                        break;
                    }
                    sink.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        }
    }

    private static final class WebSocketSession extends okhttp3.WebSocketListener {
        private final java.util.concurrent.BlockingQueue<String> messages = new java.util.concurrent.LinkedBlockingQueue<>();
        private final okhttp3.WebSocket socket;
        private volatile boolean opened;
        private volatile String failureBody;
        private volatile okhttp3.Response failureResponse;
        private volatile Throwable failure;

        WebSocketSession(OkHttpClient client, String url) {
            Request request = new Request.Builder().url(url).build();
            socket = client.newWebSocket(request, this);
        }

        boolean awaitOpen(long timeout, TimeUnit unit) {
            try {
                long deadline = System.nanoTime() + unit.toNanos(timeout);
                while (!opened && System.nanoTime() < deadline) {
                    Thread.sleep(50);
                }
                return opened;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        void sendText(String message) {
            socket.send(message);
        }

        String receiveText(long timeout, TimeUnit unit) {
            try {
                return messages.poll(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        void close() {
            socket.close(1000, "done");
        }

        String failureSummary() {
            if (failureResponse != null) {
                return "HTTP " + failureResponse.code();
            }
            if (failure != null) {
                return failure.getMessage();
            }
            return "";
        }

        boolean isTooManyClients() {
            return failureBody != null && failureBody.toLowerCase(Locale.US).contains("too many client");
        }

        @Override
        public void onOpen(okhttp3.WebSocket webSocket, okhttp3.Response response) {
            opened = true;
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, String text) {
            messages.offer(text);
        }

        @Override
        public void onFailure(okhttp3.WebSocket webSocket, Throwable t, okhttp3.Response response) {
            failure = t;
            failureResponse = response;
            if (response != null && response.body() != null) {
                try {
                    failureBody = response.body().string();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static final class Result {
        public final boolean ok;
        public final String error;

        private Result(boolean ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        public static Result ok() {
            return new Result(true, null);
        }

        public static Result error(String error) {
            return new Result(false, error);
        }
    }

}
