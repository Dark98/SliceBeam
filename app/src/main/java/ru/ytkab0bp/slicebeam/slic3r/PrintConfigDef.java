package ru.ytkab0bp.slicebeam.slic3r;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrintConfigDef {
    public static List<String> SKIP_DEFAULT_OPTIONS = Arrays.asList(
            "tilt_up_initial_speed",
            "tilt_up_finish_speed",
            "tilt_down_initial_speed",
            "tilt_down_finish_speed",
            "tower_speed",
            "thumbnails_format"
    );

    private static PrintConfigDef instance;

    private final static Map<String, Class<?>> clzMap = new HashMap<String, Class<?>>() {
        @Nullable
        @Override
        public Class<?> get(@Nullable Object key) {
            Class<?> clz = super.get(key);
            if (clz == null) {
                try {
                    put((String) key, clz = Class.forName((String) key));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
            return clz;
        }
    };
    private final static Map<Pair<Class<?>, String>, Field> fieldMap = new HashMap<Pair<Class<?>, String>, Field>() {
        @Nullable
        @Override
        public Field get(@Nullable Object key) {
            Field f = super.get(key);
            if (f == null) {
                Pair<Class<?>, String> k = (Pair<Class<?>, String>) key;
                try {
                    f = k.first.getDeclaredField(k.second);
                    f.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
            return f;
        }
    };
    private final static Map<Pair<Class<?>, String>, Object> valueMap = new HashMap<>();

    public Map<String, ConfigOptionDef> options = new HashMap<>();

    @Keep
    PrintConfigDef() {}

    @Keep
    static Object resolveEnum(String className, String value) {
        className = className.replace("/", ".");
        Class<?> clz = clzMap.get(className);
        Pair<Class<?>, String> key = new Pair<>(clz, value);
        Object val = valueMap.get(key);
        if (val != null) return val;

        Field f = fieldMap.get(key);
        try {
            valueMap.put(key, val = f.get(null));
            return val;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrintConfigDef getInstance() {
        if (instance == null) {
            Native.get_print_config_def(instance = new PrintConfigDef());
        }
        return instance;
    }

    @Keep
    void addOption(String key, ConfigOptionDef def) {
        options.put(key, def);
    }
}
