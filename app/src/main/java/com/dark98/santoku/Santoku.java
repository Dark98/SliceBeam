package com.dark98.santoku;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.instacart.truetime.time.TrueTimeImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import ru.ytkab0bp.eventbus.EventBus;
import com.dark98.santoku.boot.AppBoot;
import com.dark98.santoku.boot.BeamServerDataTask;
import com.dark98.santoku.boot.CheckUpdateJsonTask;
import com.dark98.santoku.boot.ClearModelCacheTask;
import com.dark98.santoku.boot.CloudInitTask;
import com.dark98.santoku.boot.EventBusTask;
import com.dark98.santoku.boot.LoadSlic3rConfigTask;
import com.dark98.santoku.boot.PrefsTask;
import com.dark98.santoku.boot.PrintConfigWarmupTask;
import com.dark98.santoku.boot.TrueTimeTask;
import com.dark98.santoku.boot.VibrationUtilsTask;
import com.dark98.santoku.cloud.CloudController;
import com.dark98.santoku.config.ConfigObject;
import com.dark98.santoku.slic3r.ConfigOptionDef;
import com.dark98.santoku.slic3r.PrintConfigDef;
import com.dark98.santoku.slic3r.Slic3rConfigWrapper;
import com.dark98.santoku.utils.Prefs;

public class Santoku extends Application {
    public static Santoku INSTANCE;
    public static EventBus EVENT_BUS = EventBus.newBus("main");
    public static TrueTimeImpl TRUE_TIME;
    public static Slic3rConfigWrapper CONFIG;
    public static int CONFIG_UID = 0;
    public static BeamServerData SERVER_DATA;
    public static boolean hasUpdateInfo;

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        AppBoot.run(Arrays.asList(
                new EventBusTask(),
                new PrefsTask(),
                new VibrationUtilsTask(),
                new TrueTimeTask(),
                new BeamServerDataTask(),
                new PrintConfigWarmupTask(),
                new CheckUpdateJsonTask(),
                new ClearModelCacheTask(),
                new LoadSlic3rConfigTask(),
                new CloudInitTask()
        ));
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            Prefs.getPrefs().edit().putString("crash", sw.toString()).commit();
            Intent intent = new Intent(this, SafeStartActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            Runtime.getRuntime().exit(0);
        });
    }

    public static void saveConfig() {
        if (CONFIG == null) {
            return;
        }
        Santoku.CONFIG_UID++;
        File f = getConfigFile();
        File dir = f.getParentFile();
        if (dir != null && !dir.exists()) {
            // Best effort: ensure app files dir exists before saving.
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        String serialized = CONFIG.serialize();
        try {
            File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            FileOutputStream fos = new FileOutputStream(tmp);
            fos.write(serialized.getBytes(StandardCharsets.UTF_8));
            fos.getFD().sync();
            fos.close();

            if (f.exists() && !f.delete()) {
                Log.w("Config", "Failed to delete old config before rename: " + f.getAbsolutePath());
            }
            if (!tmp.renameTo(f)) {
                // Fallback to direct write if rename fails.
                FileOutputStream direct = new FileOutputStream(f);
                direct.write(serialized.getBytes(StandardCharsets.UTF_8));
                direct.getFD().sync();
                direct.close();
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }

            // Current config should be regenerated on next slice/export.
            //noinspection ResultOfMethodCallIgnored
            getCurrentConfigFile().delete();
        } catch (Exception e) {
            Log.e("Config", "Failed to save config", e);
        }
        CloudController.notifyDataChanged();
    }

    public static File getModelCacheDir() {
        File f = new File(INSTANCE.getCacheDir(), "model");
        if (!f.exists()) f.mkdirs();
        return f;
    }

    public static File getConfigFile() {
        return new File(INSTANCE.getFilesDir(), "slic3r.ini");
    }

    public static ConfigObject buildCurrentConfigObject() {
        ConfigObject singleObject = new ConfigObject();
        ConfigObject printerConfig = Santoku.CONFIG.findPrinter(Santoku.CONFIG.presets.get("printer"));
        if (printerConfig != null) {
            singleObject.values.putAll(printerConfig.values);
        }
        ConfigObject printConfig = Santoku.CONFIG.findPrint(Santoku.CONFIG.presets.get("print"));
        if (printConfig != null) {
            for (Map.Entry<String, String> en : printConfig.values.entrySet()) {
                if (!Slic3rConfigWrapper.PRINTER_CONFIG_KEYS.contains(en.getKey())) {
                    singleObject.values.put(en.getKey(), en.getValue());
                }
            }
        }
        // TODO: MMU. Detect by printerConfig#getExtruderCount()
        ConfigObject filamentConfig = Santoku.CONFIG.findFilament(Santoku.CONFIG.presets.get("filament"));
        if (filamentConfig != null) {
            for (Map.Entry<String, String> en : filamentConfig.values.entrySet()) {
                if (!Slic3rConfigWrapper.PRINTER_CONFIG_KEYS.contains(en.getKey())) {
                    singleObject.values.put(en.getKey(), en.getValue());
                }
            }
        }

        PrintConfigDef def = PrintConfigDef.getInstance();
        for (Map.Entry<String, ConfigOptionDef> en : def.options.entrySet()) {
            if (singleObject.get(en.getKey()) == null && !PrintConfigDef.SKIP_DEFAULT_OPTIONS.contains(en.getKey()) && en.getValue().defaultValue != null) {
                singleObject.put(en.getKey(), en.getValue().defaultValue);
            }
        }
        return singleObject;
    }

    public static void genCurrentConfig() throws IOException {
        File cfg = getCurrentConfigFile();
        FileOutputStream fos = new FileOutputStream(cfg);
        ConfigObject singleObject = buildCurrentConfigObject();
        fos.write(singleObject.serialize().getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    public static File getCurrentConfigFile() {
        return new File(INSTANCE.getFilesDir(), "slic3r_current.ini");
    }
}
