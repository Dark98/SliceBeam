package com.dark98.santoku.utils;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dark98.santoku.BuildConfig;
import com.dark98.santoku.R;
import com.dark98.santoku.SetupActivity;

public class Prefs {
    public final static int CAMERA_CONTROL_MODE_ROTATE_MOVE = 0,
                            CAMERA_CONTROL_MODE_MOVE_ROTATE = 1,
                            CAMERA_CONTROL_MODE_MOVE_ONLY = 2;

    private static SharedPreferences mPrefs;

    public static void init(Application ctx) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static SharedPreferences getPrefs() {
        return mPrefs;
    }

    public static String getLastCommit() {
        return mPrefs.getString("last_commit", null);
    }

    public static void setLastCommit() {
        mPrefs.edit().putString("last_commit", BuildConfig.COMMIT).apply();
    }

    public static boolean isScaleInputInMM() {
        return mPrefs.getBoolean("scale_input_mm", false);
    }

    public static void setScaleInputInMM(boolean v) {
        mPrefs.edit().putBoolean("scale_input_mm", v).apply();
    }

    public static boolean isScaleLinked() {
        return mPrefs.getBoolean("scale_linked", true);
    }

    public static void setScaleLinked(boolean v) {
        mPrefs.edit().putBoolean("scale_linked", v).apply();
    }

    public static long getLastCheckedInfo() {
        return mPrefs.getLong("last_checked_info", 0);
    }

    public static void setLastCheckedInfo() {
        mPrefs.edit().putLong("last_checked_info", System.currentTimeMillis()).apply();
    }

    // Only used for displaying Boosty info, nothing more
    public static boolean isRussianIP() {
        return mPrefs.getBoolean("russian_ip", false);
    }

    public static void setRussianIP(boolean v) {
        mPrefs.edit().putBoolean("russian_ip", v).apply();
    }

    public static void setBeamServerData(String data) {
        mPrefs.edit().putString("beam_server_data", data).apply();
    }

    public static String getBeamServerData() {
        return mPrefs.getString("beam_server_data", "{}");
    }

    public static int getCameraControlMode() {
        return mPrefs.getInt("camera_control_mode", mPrefs.getBoolean("rotation_enabled", true) ? CAMERA_CONTROL_MODE_ROTATE_MOVE : CAMERA_CONTROL_MODE_MOVE_ONLY);
    }

    public static void setCameraControlMode(int mode) {
        mPrefs.edit().putInt("camera_control_mode", mode).apply();
    }

    public static boolean isOrthoProjectionEnabled() {
        return mPrefs.getBoolean("ortho_projection", true);
    }

    public static void setOrthoProjectionEnabled(boolean e) {
        mPrefs.edit().putBoolean("ortho_projection", e).apply();
    }

    public static float getCameraSensitivity() {
        return 5f;
    }

    public static int getAccentColor() {
        return mPrefs.getInt("accent", SetupActivity.AccentColors.DEFAULT.color);
    }

    public static void setAccentColor(int color) {
        mPrefs.edit().putInt("accent", color).apply();
    }

    public static boolean isVibrationEnabled() {
        return mPrefs.getBoolean("vibration", true);
    }

    public static float getRenderScale() {
        return mPrefs.getFloat("render_scale", 1f);
    }

    public static void setRenderScale(float s) {
        mPrefs.edit().putFloat("render_scale", s).apply();
    }

    private static ThemeMode cachedThemeMode;
    public static ThemeMode getThemeMode() {
        if (cachedThemeMode == null) {
            cachedThemeMode = ThemeMode.values()[mPrefs.getInt("theme_mode", 0)];
        }
        return cachedThemeMode;
    }

    public static void setThemeMode(int i) {
        mPrefs.edit().putInt("theme_mode", i).apply();
        cachedThemeMode = null;
    }

    public static String getCloudAPIToken() {
        return mPrefs.getString("cloud_api_token", null);
    }

    public static void setCloudAPIToken(String token) {
        SharedPreferences.Editor e = mPrefs.edit();
        if (token == null) {
            e.remove("cloud_api_token");
        } else {
            e.putString("cloud_api_token", token);
        }
        e.apply();
    }

    public static boolean isCloudProfileSyncEnabled() {
        return mPrefs.getBoolean("cloud_profile_sync", true);
    }

    public static void setCloudProfileSyncEnabled(boolean en) {
        mPrefs.edit().putBoolean("cloud_profile_sync", en).apply();
    }

    public static String getCloudCachedUserInfo() {
        return mPrefs.getString("cloud_cached_user_info", null);
    }

    public static void setCloudCachedUserInfo(String info) {
        SharedPreferences.Editor e = mPrefs.edit();
        if (info == null) {
            e.remove("cloud_cached_user_info");
        } else {
            e.putString("cloud_cached_user_info", info);
        }
        e.apply();
    }

    public static int getCloudCachedUsedModels() {
        return mPrefs.getInt("cloud_cached_models_used", 0);
    }

    public static int getCloudCachedMaxModels() {
        return mPrefs.getInt("cloud_cached_models_max", 50);
    }

    public static void setCloudCachedUsedMaxModels(int used, int max) {
        mPrefs.edit().putInt("cloud_cached_models_used", used).putInt("cloud_cached_models_max", max).apply();
    }

    public static String getCloudCachedUserFeatures() {
        return mPrefs.getString("cloud_cached_user_features", null);
    }

    public static void setCloudCachedUserFeatures(String features) {
        SharedPreferences.Editor e = mPrefs.edit();
        if (features == null) {
            e.remove("cloud_cached_user_features");
        } else {
            e.putString("cloud_cached_user_features", features);
        }
        e.apply();
    }

    public static long getCloudLastFeaturesSync() {
        return mPrefs.getLong("cloud_last_features_sync", 0);
    }

    public static void setCloudLastFeaturesSync(long ls) {
        mPrefs.edit().putLong("cloud_last_features_sync", ls).apply();
    }

    public static long getCloudLastSync() {
        return mPrefs.getLong("cloud_last_sync", 0);
    }

    public static void setCloudLastSync(long ls) {
        mPrefs.edit().putLong("cloud_last_sync", ls).apply();
    }

    public static long getCloudLocalLastSentModified() {
        return mPrefs.getLong("cloud_local_last_sent_modified", 0);
    }

    public static void setCloudLocalLastSentModified(long lm) {
        mPrefs.edit().putLong("cloud_local_last_sent_modified", lm).apply();
    }

    public static long getCloudLocalLastModified() {
        return mPrefs.getLong("cloud_local_last_modified", 0);
    }

    public static void setCloudLocalLastModified(long lm) {
        mPrefs.edit().putLong("cloud_local_last_modified", lm).apply();
    }

    public static long getCloudRemoteLastModified() {
        return mPrefs.getLong("cloud_remote_last_modified", 0);
    }

    public static void setCloudRemoteLastModified(long lm) {
        mPrefs.edit().putLong("cloud_remote_last_modified", lm).apply();
    }

    public enum ThemeMode {
        SYSTEM(R.string.SettingsInterfaceThemeSystem),
        LIGHT(R.string.SettingsInterfaceThemeLight),
        DARK(R.string.SettingsInterfaceThemeDark);

        public final int title;

        ThemeMode(int title) {
            this.title = title;
        }
    }
}
