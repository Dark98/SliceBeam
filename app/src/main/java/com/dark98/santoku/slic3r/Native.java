package com.dark98.santoku.slic3r;

import com.dark98.santoku.Santoku;

class Native {
    static {
        System.loadLibrary("c++_shared");
        System.loadLibrary("gmp");
        System.loadLibrary("gmpxx");
        System.loadLibrary("mpfr");
        OCCTLoader.load();

        System.loadLibrary("slic3r");

        set_svg_path_prefix(Santoku.INSTANCE.getCacheDir().getAbsolutePath());
    }

    static native void get_print_config_def(PrintConfigDef def);

    static native void set_svg_path_prefix(String prefix);

    static native long shader_init_from_texts(String name, String fragment, String vertex);
    static native int shader_get_id(long ptr);
    static native int shader_get_uniform_location(long ptr, String name);
    static native int shader_get_attrib_location(long ptr, String name);
    static native void shader_start_using(long ptr);
    static native void shader_stop_using(long ptr);
    static native void shader_release(long ptr);

    static native long glmodel_create();
    static native void glmodel_init_from_model(long ptr, long model);
    static native void glmodel_init_from_model_object(long ptr, long model, int i);
    static native void glmodel_init_raycast_data(long ptr);
    static native void glmodel_set_color(long ptr, float red, float green, float blue, float alpha);
    static native void glmodel_render(long ptr);
    static native void glmodel_stilized_arrow(long ptr, float tipRadius, float tipLength, float stemRadius, float stemLength);
    static native void glmodel_init_background_triangles(long ptr);
    static native void glmodel_init_bounding_box(long ptr, long modelPtr, int i);
    static native boolean glmodel_is_initialized(long ptr);
    static native boolean glmodel_is_empty(long ptr);
    static native double[] glmodel_raycast_closest_hit(long ptr, double[] point, double[] direction);
    static native void glmodel_reset(long ptr);
    static native void glmodel_release(long ptr);

    static native long bed_create(long[] data);
    static native int bed_get_bounding_volume_max_size(long ptr);
    static native double[] bed_get_bounding_volume(long ptr);
    static native void bed_configure(long ptr, String configPath);
    static native void bed_init_triangles_mesh(long ptr, long triangles);
    static native boolean bed_arrange(long ptr, long modelPtr);
    static native void bed_release(long ptr);

    static native long models_merge(long... ptrs);
    static native long model_create();
    static native long model_read_from_file(String path, String baseName) throws Slic3rRuntimeError;
    static native int model_get_objects_count(long ptr);
    static native void model_add_object_from_another(long ptr, long from, int i);
    static native void model_delete_object(long ptr, int i);
    static native double[] model_get_translation(long ptr, int objectIndex);
    static native double[] model_get_scale(long ptr, int objectIndex);
    static native double[] model_get_mirror(long ptr, int objectIndex);
    static native double[] model_get_rotation(long ptr, int objectIndex);
    static native double[] model_get_bounding_box_exact(long ptr, int i);
    static native double[] model_get_bounding_box_approx(long ptr, int i);
    static native double[] model_get_bounding_box_exact_global(long ptr);
    static native double[] model_get_bounding_box_approx_global(long ptr);
    static native boolean model_is_left_handed(long ptr, int i);
    static native void model_translate(long ptr, int i, double x, double y, double z);
    static native void model_translate_global(long ptr, double x, double y, double z);
    static native void model_ensure_on_bed(long ptr, int i);
    static native void model_scale(long ptr, int i, double x, double y, double z);
    static native void model_rotate(long ptr, int i, double x, double y, double z);
    static native void model_flatten_rotate(long ptr, int i, long surfacePtr);
    static native long[] model_create_flatten_planes(long ptr, int i);
    static native void model_auto_orient(long ptr, int i);
    static native boolean model_is_big_object(long ptr, int i);
    static native int model_get_extruder(long ptr, int i);
    static native void model_set_extruder(long ptr, int i, int extruder);
    static native long model_slice(long ptr, String configPath, String path, SliceListener listener) throws Slic3rRuntimeError;
    static native void model_export_3mf(long ptr, String configPath, String path) throws Slic3rRuntimeError;
    static native void model_release(long ptr);

    static native long gcoderesult_load_file(String path, String name);
    static native String gcoderesult_get_recommended_name(long ptr);
    static native double gcoderesult_get_used_filament_mm(long ptr, int role);
    static native double gcoderesult_get_used_filament_g(long ptr, int role);
    static native void gcoderesult_release(long ptr);

    static native long vgcode_create();
    static native void vgcode_init(long ptr);
    static native boolean vgcode_is_initialized(long ptr);
    static native void vgcode_set_colors(long ptr, int[] colors);
    static native long vgcode_get_layers_count(long ptr);
    static native void vgcode_load(long ptr, long resultPtr);
    static native void vgcode_render(long ptr, float[] viewMatrix, float[] projectionMatrix);
    static native void vgcode_set_layers_view_range(long ptr, long min, long max);
    static native long[] vgcode_get_layers_view_range(long ptr);
    static native float vgcode_get_estimated_time(long ptr);
    static native float vgcode_get_estimated_time_role(long ptr, int role);
    static native boolean vgcode_is_extrusion_role_visible(long ptr, int role);
    static native void vgcode_toggle_extrusion_role_visibility(long ptr, int role);
    static native void vgcode_reset(long ptr);
    static native void vgcode_release(long ptr);

    static native long utils_config_create(String config);
    static native boolean utils_config_check_compatibility(long ptr, String condition);
    static native String utils_config_eval(long ptr, String condition) throws Slic3rRuntimeError;
    static native void utils_config_release(long ptr);

    static native void utils_calc_view_normal_matrix(double[] viewMatrix, double[] worldMatrix, double[] normalMatrix);
    static native double[] utils_unproject(double[] viewMatrix, double[] projectionMatrix, int screenWidth, int screenHeight, double x, double y);
}
