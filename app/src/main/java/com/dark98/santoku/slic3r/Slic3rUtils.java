package com.dark98.santoku.slic3r;

import static com.dark98.santoku.utils.DebugUtils.assertTrue;

import android.text.TextUtils;

import com.dark98.santoku.utils.Vec3d;

public class Slic3rUtils {
    public static void calcViewNormalMatrix(double[] viewMatrix, double[] worldMatrix, double[] normalMatrix) {
        assertTrue(viewMatrix.length == 16);
        assertTrue(worldMatrix.length == 16);
        assertTrue(normalMatrix.length == 12);

        Native.utils_calc_view_normal_matrix(viewMatrix, worldMatrix, normalMatrix);
    }

    public static Vec3d unproject(double[] viewMatrix, double[] projectionMatrix, int screenWidth, int screenHeight, double x, double y) {
        assertTrue(viewMatrix.length == 16);
        assertTrue(projectionMatrix.length == 16);

        double[] v = Native.utils_unproject(viewMatrix, projectionMatrix, screenWidth, screenHeight, x, y);
        return new Vec3d(v[0], v[1], v[2]);
    }

    public final static class ConfigChecker {
        private final long pointer;

        public ConfigChecker(String config) {
            pointer = Native.utils_config_create(config);
        }

        public boolean checkCompatibility(String condition) {
            if (TextUtils.isEmpty(condition)) return true;
            return Native.utils_config_check_compatibility(pointer, condition);
        }

        public String eval(String condition) throws Slic3rRuntimeError {
            return Native.utils_config_eval(pointer, condition);
        }

        public void release() {
            Native.utils_config_release(pointer);
        }
    }
}
