package com.dark98.santoku.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dark98.santoku.Santoku;
import com.dark98.santoku.config.ConfigObject;

public class IOUtils {
    public static ExecutorService IO_POOL = Executors.newCachedThreadPool();

    public static String getDisplayName(Uri uri) {
        ContentResolver resolver = Santoku.INSTANCE.getContentResolver();

        String fileName = null;
        Cursor metaCursor = null;
        try {
            String[] projection = {OpenableColumns.DISPLAY_NAME};
            metaCursor = resolver.query(uri, projection, null, null, null);
            if (metaCursor != null && metaCursor.moveToFirst()) {
                fileName = metaCursor.getString(0);
            }
        } catch (Exception ignored) {
            // Some providers throw for query; fall back below.
        } finally {
            if (metaCursor != null) {
                metaCursor.close();
            }
        }

        if (fileName == null) {
            try {
                String path = uri.getPath();
                if (path != null) {
                    int idx = path.lastIndexOf('/');
                    if (idx != -1 && idx + 1 < path.length()) {
                        fileName = path.substring(idx + 1);
                    } else if (!path.isEmpty()) {
                        fileName = path;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return fileName;
    }
    public static String readString(InputStream in) throws IOException {
        return readString(in, false);
    }

    public static String readString(InputStream in, boolean close) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240]; int c;
        while ((c = in.read(buffer)) != -1) {
            bos.write(buffer, 0, c);
        }
        if (close) {
            in.close();
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String configJsonToString(Object obj) throws JSONException {
        if (obj instanceof JSONArray) {
            StringBuilder sb = new StringBuilder();
            JSONArray arr = (JSONArray) obj;
            for (int i = 0; i < arr.length(); i++) {
                if (sb.length() != 0) sb.append(",");
                sb.append(arr.getString(i));
            }
            return sb.toString();
        } else {
            return obj.toString();
        }
    }

    private static ConfigObject downloadProfilesRecursively(String vendor, String type, String profile, List<String> supportedKeys) throws IOException, JSONException, MissingProfileException {
        ConfigObject cfg = new ConfigObject();

        HttpURLConnection con = (HttpURLConnection) new URL(String.format("https://raw.githubusercontent.com/SoftFever/OrcaSlicer/main/resources/profiles/%s/%s/%s.json", vendor, type, profile)).openConnection();
        if (con.getResponseCode() == 404) {
            throw new MissingProfileException(profile);
        }
        JSONObject obj = new JSONObject(readString(con.getInputStream()));
        if (!TextUtils.isEmpty(obj.optString("inherits", null))) {
            ConfigObject o = downloadProfilesRecursively(vendor, type, obj.getString("inherits"), supportedKeys);

            for (Map.Entry<String, String> en : o.values.entrySet()) {
                if (supportedKeys.contains(en.getKey())) {
                    if (en.getKey().equals("ironing_type") && en.getValue().equals("no ironing")) {
                        cfg.values.put("ironing", "0");
                        cfg.values.put("ironing_type", "top");
                    } else if (!en.getKey().equals("thumbnails")) {
                        cfg.values.put(en.getKey(), en.getValue());
                    }
                }
            }
        }

        for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
            String key = it.next();

            if (key.equals("print_settings_id") || key.equals("filament_settings_id") || key.equals("printer_settings_id")) {
                cfg.setTitle(obj.getString(key));
            } else if (!key.equals("inherits")) {
                cfg.put(key, configJsonToString(obj.get(key)));
            }
        }
        return cfg;
    }

    public static ConfigObject configJsonToIni(JSONObject obj, String type, List<String> supportedKeys, List<String> inBundle) throws JSONException, IOException, MissingProfileException {
        ConfigObject cfg = new ConfigObject();
        if (!TextUtils.isEmpty(obj.optString("inherits", null))) {
            String inherit = obj.getString("inherits");

            if (inBundle.contains(inherit)) {
                // Will do it later then
                cfg.put("inherits", inherit);
            } else if (inherit.indexOf(' ') == -1) {
                throw new MissingProfileException(inherit);
            } else {
                String vendor;
                if (inherit.indexOf(' ') == -1) {
                    throw new MissingProfileException(inherit);
                }
                if (inherit.contains("@BBL")) {
                    vendor = "BBL";
                } else if (type.equals("process")) {
                    int i = inherit.indexOf('@') + 1;
                    int j = inherit.indexOf(' ', i);
                    if (j == -1) j = inherit.length();
                    vendor = inherit.substring(i, j);
                } else {
                    vendor = inherit.substring(0, inherit.indexOf(' '));
                }

                if (vendor.equals("Generic") || inherit.startsWith("Bambu Lab")) vendor = "BBL";

                ConfigObject _obj = downloadProfilesRecursively(vendor, type, inherit, supportedKeys);
                for (Map.Entry<String, String> en : _obj.values.entrySet()) {
                    String key = en.getKey();
                    switch (key) {
                        case "machine_start_gcode":
                            key = "start_gcode";
                            break;
                        case "machine_end_gcode":
                            key = "end_gcode";
                            break;
                        case "printable_area":
                            key = "bed_shape";
                            break;
                        case "printable_height":
                            key = "max_print_height";
                            break;
                        case "layer_change_gcode":
                            key = "layer_gcode";
                            break;
                        case "before_layer_change_gcode":
                            key = "before_layer_gcode";
                            break;
                        case "filament_start_gcode":
                            key = "start_filament_gcode";
                            break;
                        case "filament_end_gcode":
                            key = "end_filament_gcode";
                            break;
                        case "retraction_minimum_level":
                            key = "retract_before_travel";
                            break;
                        case "retraction_length":
                            key = "retract_length";
                            break;
                        case "retraction_speed":
                            key = "retract_speed";
                            break;
                        case "deretraction_speed":
                            key = "deretract_speed";
                            break;
                        case "change_filament_gcode":
                            key = "pause_print_gcode";
                            break;
                        case "nozzle_temperature":
                            key = "temperature";
                            break;
                        case "nozzle_temperature_initial_layer":
                            key = "first_layer_temperature";
                            break;
                        case "filament_flow_ratio":
                            key = "extrusion_multiplier";
                            break;
                        case "chamber_temperatures":
                            key = "chamber_temperature";
                            break;
                        case "fan_max_speed":
                            key = "max_fan_speed";
                            break;
                        case "fan_min_speed":
                            key = "min_fan_speed";
                            break;
                        case "overhang_fan_speed":
                            key = "bridge_fan_speed";
                            break;
                        case "slow_down_layer_time":
                            key = "slowdown_below_layer_time";
                            break;
                        case "slow_down_min_speed":
                            key = "min_print_speed";
                            break;
                    }

                    if (key.equals("pressure_advance")) {
                        StringBuilder sb = new StringBuilder("SET_PRESSURE_ADVANCE ADVANCE=").append(en.getValue());
                        if (cfg.values.containsKey("start_filament_gcode")) {
                            sb.append("\n").append(cfg.get("start_filament_gcode"));
                        }
                        cfg.values.put("start_filament_gcode", sb.toString());
                    }

                    if (supportedKeys.contains(key)) {
                        if (key.equals("ironing_type") && en.getValue().equals("no ironing")) {
                            cfg.values.put("ironing", "0");
                            cfg.values.put("ironing_type", "top");
                        }
                        if (key.equals("start_filament_gcode") || key.equals("end_filament_gcode") ||
                            key.equals("start_gcode") || key.equals("end_gcode")) {

                            String val = en.getValue();
                            if (key.equals("start_filament_gcode")) {
                                if (cfg.values.containsKey("start_filament_gcode")) {
                                    val = cfg.get("start_filament_gcode") + "\n" + val;
                                }
                            }

                            cfg.values.put(key, val.replaceAll("(\\{|\\[)nozzle_temperature_initial_layer(\\[\\d+]|)(}|])", "$1first_layer_temperature$2$3")
                                    .replaceAll("(\\{|\\[)bed_temperature_initial_layer_single(\\[\\d+]|)(}|])", "$1first_layer_bed_temperature$2$3"));
                        } else if (!key.equals("thumbnails")) {
                            cfg.values.put(key, en.getValue());
                        }
                    }
                }
            }
        }

        for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
            String key = it.next();

            if (key.equals("print_settings_id") || key.equals("filament_settings_id") || key.equals("printer_settings_id")) {
                String v = obj.getString(key);
                if (v.startsWith("[\"") && v.endsWith("\"]")) v = v.substring(2, v.length() - 2);
                cfg.setTitle(v);
            } else if (!key.equals("inherits") && supportedKeys.contains(key)) {
                cfg.put(key, configJsonToString(obj.get(key)));
            }
        }
        return cfg;
    }

    public static class MissingProfileException extends Exception {
        public final String profile;

        public MissingProfileException(String profile) {
            this.profile = profile;
        }

        @Override
        public String toString() {
            return "MissingProfileException{" +
                    "profile='" + profile + '\'' +
                    '}';
        }
    }
}
