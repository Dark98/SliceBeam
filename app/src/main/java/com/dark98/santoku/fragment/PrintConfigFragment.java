package com.dark98.santoku.fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.dark98.santoku.R;
import com.dark98.santoku.Santoku;
import com.dark98.santoku.config.ConfigObject;
import com.dark98.santoku.recycler.SpaceItem;
import com.dark98.santoku.slic3r.PrintConfigDef;
import com.dark98.santoku.slic3r.Slic3rUtils;
import com.dark98.santoku.utils.ViewUtils;

public class PrintConfigFragment extends ProfileListFragment {
    private List<ProfileListItem> compatItems;
    private String lastPrinter;
    private int lastUid;

    private ConfigObject currentConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        onResetConfig();
    }

    @Override
    protected List<ProfileListItem> getItems(boolean filter) {
        List<ConfigObject> list = Santoku.CONFIG.printConfigs;
        if (filter) {
            String printer = Santoku.CONFIG.presets.get("printer");
            if (Objects.equals(lastPrinter, printer) && compatItems != null && lastUid == Santoku.CONFIG_UID) {
                return compatItems;
            }

            List<ConfigObject> nList = new ArrayList<>(list.size());
            Slic3rUtils.ConfigChecker checker = new Slic3rUtils.ConfigChecker(Santoku.CONFIG.findPrinter(printer).serialize());
            for (ConfigObject obj : list) {
                if (checker.checkCompatibility(obj.get("compatible_printers_condition"))) {
                    nList.add(obj);
                }
            }
            checker.release();
            lastPrinter = printer;
            lastUid = Santoku.CONFIG_UID;
            return compatItems = (List) nList;
        }
        return (List) list;
    }

    @Override
    protected List<OptionElement> getConfigItems() {
        PrintConfigDef def = PrintConfigDef.getInstance();
        return Arrays.asList(
                new OptionElement(R.drawable.print_layers_28, "Layers and perimeters"),
                new OptionElement(new SubHeader("Layer height")),
                new OptionElement(def.options.get("layer_height")),
                new OptionElement(def.options.get("first_layer_height")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Vertical shells")),
                new OptionElement(def.options.get("perimeters")),
                new OptionElement(def.options.get("spiral_vase")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Horizontal shells")),
                new OptionElement(def.options.get("top_solid_layers")),
                new OptionElement(def.options.get("bottom_solid_layers")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Minimum shell thickness")),
                new OptionElement(def.options.get("top_solid_min_thickness")),
                new OptionElement(def.options.get("bottom_solid_min_thickness")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Quality (slower slicing)")),
                new OptionElement(def.options.get("extra_perimeters")),
                new OptionElement(def.options.get("extra_perimeters_on_overhangs")),
                new OptionElement(def.options.get("avoid_crossing_curled_overhangs")),
                new OptionElement(def.options.get("avoid_crossing_perimeters")),
                new OptionElement(def.options.get("avoid_crossing_perimeters_max_detour")),
                new OptionElement(def.options.get("thin_walls")),
                new OptionElement(def.options.get("thick_bridges")),
                new OptionElement(def.options.get("overhangs")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Advanced")),
                new OptionElement(def.options.get("seam_position")),
                new OptionElement(def.options.get("staggered_inner_seams")),
                new OptionElement(def.options.get("external_perimeters_first")),
                new OptionElement(def.options.get("gap_fill_enabled")),
                new OptionElement(def.options.get("perimeter_generator")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Fuzzy skin (experimental)")),
                new OptionElement(def.options.get("fuzzy_skin")),
                new OptionElement(def.options.get("fuzzy_skin_thickness")),
                new OptionElement(def.options.get("fuzzy_skin_point_dist")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Only one perimeter")),
                new OptionElement(def.options.get("top_one_perimeter_type")),
                new OptionElement(def.options.get("only_one_perimeter_first_layer")),

                new OptionElement(R.drawable.print_infill_28, "Infill"),
                new OptionElement(new SubHeader("Infill")),
                new OptionElement(def.options.get("fill_density")),
                new OptionElement(def.options.get("fill_pattern")),
                new OptionElement(def.options.get("infill_anchor")),
                new OptionElement(def.options.get("infill_anchor_max")),
                new OptionElement(def.options.get("top_fill_pattern")),
                new OptionElement(def.options.get("bottom_fill_pattern")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Ironing")),
                new OptionElement(def.options.get("ironing")),
                new OptionElement(def.options.get("ironing_type")),
                new OptionElement(def.options.get("ironing_flowrate")),
                new OptionElement(def.options.get("ironing_spacing")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Reducing printing time")),
                new OptionElement(def.options.get("infill_every_layers")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Advanced")),
                new OptionElement(def.options.get("solid_infill_every_layers")),
                new OptionElement(def.options.get("fill_angle")),
                new OptionElement(def.options.get("solid_infill_below_area")),
                new OptionElement(def.options.get("bridge_angle")),
                new OptionElement(def.options.get("only_retract_when_crossing_perimeters")),
                new OptionElement(def.options.get("infill_first")),

                new OptionElement(R.drawable.print_skirt_28, "Skirt and brim"),
                new OptionElement(new SubHeader("Skirt")),
                new OptionElement(def.options.get("skirts")),
                new OptionElement(def.options.get("skirt_distance")),
                new OptionElement(def.options.get("skirt_height")),
                new OptionElement(def.options.get("draft_shield")),
                new OptionElement(def.options.get("min_skirt_length")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Brim")),
                new OptionElement(def.options.get("brim_type")),
                new OptionElement(def.options.get("brim_width")),
                new OptionElement(def.options.get("brim_separation")),

                new OptionElement(R.drawable.print_support_28, "Support material"),
                new OptionElement(new SubHeader("Support material")),
                new OptionElement(def.options.get("support_material")),
                new OptionElement(def.options.get("support_material_auto")),
                new OptionElement(def.options.get("support_material_threshold")),
                new OptionElement(def.options.get("support_material_enforce_layers")),
                new OptionElement(def.options.get("raft_first_layer_density")),
                new OptionElement(def.options.get("raft_first_layer_expansion")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Raft")),
                new OptionElement(def.options.get("raft_layers")),
                new OptionElement(def.options.get("raft_contact_distance")),
                new OptionElement(def.options.get("raft_expansion")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Options for support material and raft")),
                new OptionElement(def.options.get("support_material_style")),
                new OptionElement(def.options.get("support_material_contact_distance")),
                new OptionElement(def.options.get("support_material_bottom_contact_distance")),
                new OptionElement(def.options.get("support_material_pattern")),
                new OptionElement(def.options.get("support_material_with_sheath")),
                new OptionElement(def.options.get("support_material_spacing")),
                new OptionElement(def.options.get("support_material_angle")),
                new OptionElement(def.options.get("support_material_closing_radius")),
                new OptionElement(def.options.get("support_material_interface_layers")),
                new OptionElement(def.options.get("support_material_bottom_interface_layers")),
                new OptionElement(def.options.get("support_material_interface_pattern")),
                new OptionElement(def.options.get("support_material_interface_spacing")),
                new OptionElement(def.options.get("support_material_interface_contact_loops")),
                new OptionElement(def.options.get("support_material_buildplate_only")),
                new OptionElement(def.options.get("support_material_xy_spacing")),
                new OptionElement(def.options.get("dont_support_bridges")),
                new OptionElement(def.options.get("support_material_synchronize_layers")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Organic supports")),
                new OptionElement(def.options.get("support_tree_angle")),
                new OptionElement(def.options.get("support_tree_angle_slow")),
                new OptionElement(def.options.get("support_tree_branch_diameter")),
                new OptionElement(def.options.get("support_tree_branch_diameter_angle")),
                new OptionElement(def.options.get("support_tree_branch_diameter_double_wall")),
                new OptionElement(def.options.get("support_tree_tip_diameter")),
                new OptionElement(def.options.get("support_tree_branch_distance")),
                new OptionElement(def.options.get("support_tree_top_rate")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(R.drawable.clock_outline_28, "Speed"),
                new OptionElement(new SubHeader("Speed for print moves")),
                new OptionElement(def.options.get("perimeter_speed")),
                new OptionElement(def.options.get("small_perimeter_speed")),
                new OptionElement(def.options.get("external_perimeter_speed")),
                new OptionElement(def.options.get("infill_speed")),
                new OptionElement(def.options.get("solid_infill_speed")),
                new OptionElement(def.options.get("top_solid_infill_speed")),
                new OptionElement(def.options.get("support_material_speed")),
                new OptionElement(def.options.get("support_material_interface_speed")),
                new OptionElement(def.options.get("bridge_speed")),
                new OptionElement(def.options.get("gap_fill_speed")),
                new OptionElement(def.options.get("ironing_speed")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Dynamic overhang speed")),
                new OptionElement(def.options.get("overhang_speed_0")),
                new OptionElement(def.options.get("overhang_speed_1")),
                new OptionElement(def.options.get("overhang_speed_2")),
                new OptionElement(def.options.get("overhang_speed_3")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Speed for non-print moves")),
                new OptionElement(def.options.get("travel_speed")),
                new OptionElement(def.options.get("travel_speed_z")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Modifiers")),
                new OptionElement(def.options.get("first_layer_speed")),
                new OptionElement(def.options.get("first_layer_speed_over_raft")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Acceleration control (advanced)")),
                new OptionElement(def.options.get("external_perimeter_acceleration")),
                new OptionElement(def.options.get("top_solid_infill_acceleration")),
                new OptionElement(def.options.get("solid_infill_acceleration")),
                new OptionElement(def.options.get("infill_acceleration")),
                new OptionElement(def.options.get("bridge_acceleration")),
                new OptionElement(def.options.get("first_layer_acceleration")),
                new OptionElement(def.options.get("first_layer_acceleration_over_raft")),
                new OptionElement(def.options.get("wipe_tower_acceleration")),
                new OptionElement(def.options.get("travel_acceleration")),
                new OptionElement(def.options.get("default_acceleration")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Autospeed (advanced)")),
                new OptionElement(def.options.get("max_print_speed")),
                new OptionElement(def.options.get("max_volumetric_speed")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Pressure equalizer (experimental)")),
                new OptionElement(def.options.get("max_volumetric_extrusion_rate_slope_positive")),
                new OptionElement(def.options.get("max_volumetric_extrusion_rate_slope_negative")),

                new OptionElement(R.drawable.hashtag_outline_28, "Multiple Extruders"),
                new OptionElement(new SubHeader("Extruders")),
                new OptionElement(def.options.get("perimeter_extruder")),
                new OptionElement(def.options.get("infill_extruder")),
                new OptionElement(def.options.get("solid_infill_extruder")),
                new OptionElement(def.options.get("support_material_extruder")),
                new OptionElement(def.options.get("support_material_interface_extruder")),
                new OptionElement(def.options.get("wipe_tower_extruder")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Ooze prevention")),
                new OptionElement(def.options.get("ooze_prevention")),
                new OptionElement(def.options.get("standby_temperature_delta")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Wipe tower")),
                new OptionElement(def.options.get("wipe_tower")),
                new OptionElement(def.options.get("wipe_tower_x")),
                new OptionElement(def.options.get("wipe_tower_y")),
                new OptionElement(def.options.get("wipe_tower_width")),
                new OptionElement(def.options.get("wipe_tower_rotation_angle")),
                new OptionElement(def.options.get("wipe_tower_brim_width")),
                new OptionElement(def.options.get("wipe_tower_bridging")),
                new OptionElement(def.options.get("wipe_tower_cone_angle")),
                new OptionElement(def.options.get("wipe_tower_extra_spacing")),
                new OptionElement(def.options.get("wipe_tower_extra_flow")),
                new OptionElement(def.options.get("wipe_tower_no_sparse_layers")),
                new OptionElement(def.options.get("single_extruder_multi_material_priming")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Advanced")),
                new OptionElement(def.options.get("interface_shells")),
                new OptionElement(def.options.get("mmu_segmented_region_max_width")),
                new OptionElement(def.options.get("mmu_segmented_region_interlocking_depth")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(R.drawable.settings_outline_28, "Advanced"),
                new OptionElement(new SubHeader("Extrusion width")),
                new OptionElement(def.options.get("extrusion_width")),
                new OptionElement(def.options.get("first_layer_extrusion_width")),
                new OptionElement(def.options.get("perimeter_extrusion_width")),
                new OptionElement(def.options.get("external_perimeter_extrusion_width")),
                new OptionElement(def.options.get("infill_extrusion_width")),
                new OptionElement(def.options.get("solid_infill_extrusion_width")),
                new OptionElement(def.options.get("top_infill_extrusion_width")),
                new OptionElement(def.options.get("support_material_extrusion_width")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Overlap")),
                new OptionElement(def.options.get("infill_overlap")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Flow")),
                new OptionElement(def.options.get("bridge_flow_ratio")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Slicing")),
                new OptionElement(def.options.get("slice_closing_radius")),
                new OptionElement(def.options.get("slicing_mode")),
                new OptionElement(def.options.get("resolution")),
                new OptionElement(def.options.get("gcode_resolution")),
                new OptionElement(def.options.get("arc_fitting")),
                // TODO: Support x_size_compensation/y_size_compensation instead (I'm too lazy to write description for now)
//                new OptionElement(def.options.get("x_size_compensation")),
//                new OptionElement(def.options.get("y_size_compensation")),
                new OptionElement(def.options.get("xy_size_compensation")),
                new OptionElement(def.options.get("elefant_foot_compensation")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Arachne perimeter generator")),
                new OptionElement(def.options.get("wall_transition_angle")),
                new OptionElement(def.options.get("wall_transition_filter_deviation")),
                new OptionElement(def.options.get("wall_transition_length")),
                new OptionElement(def.options.get("wall_distribution_count")),
                new OptionElement(def.options.get("min_bead_width")),
                new OptionElement(def.options.get("min_feature_size")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(R.drawable.door_arrow_right_outline_28, "Output options"),
                new OptionElement(new SubHeader("Sequential printing")),
                new OptionElement(def.options.get("complete_objects")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Extruder clearance")),
                new OptionElement(def.options.get("extruder_clearance_radius")),
                new OptionElement(def.options.get("extruder_clearance_height")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Output file")),
                new OptionElement(def.options.get("gcode_comments")),
                new OptionElement(def.options.get("gcode_label_objects")),
                new OptionElement(def.options.get("output_filename_format")),

                new OptionElement(R.drawable.note_pen_outline_96, "Notes"),
                new OptionElement(new SubHeader("Notes")),
                new OptionElement(def.options.get("notes")),

                new OptionElement(R.drawable.wrench_outline_28, "Dependencies"),
                new OptionElement(new SubHeader("Profile dependencies")),
                new OptionElement(def.options.get("compatible_printers")),
                new OptionElement(def.options.get("compatible_printers_condition"))
        );
    }

    @Override
    protected void cloneCurrentProfile() {
        ConfigObject obj = new ConfigObject(Santoku.INSTANCE.getString(R.string.SettingsProfileCopy, currentConfig.getTitle()));
        obj.values.putAll(currentConfig.values);
        currentConfig = new ConfigObject(obj);

        Santoku.CONFIG.printConfigs.add(obj);
        Santoku.CONFIG.presets.put("print", obj.getTitle());
        Santoku.saveConfig();
        Santoku.getCurrentConfigFile().delete();

        currentConfig = new ConfigObject(obj);
        dropdownView.setTitle(getCurrentConfig().getTitle());
        compatItems = null;
    }

    @Override
    protected void deleteCurrentProfile() {
        compatItems = null;
        Santoku.CONFIG.printConfigs.remove(Santoku.CONFIG.findPrint(currentConfig.getTitle()));
        selectItem(getItems(true).get(0));

        dropdownView.setTitle(getCurrentConfig().getTitle());
    }

    @Override
    protected void onApplyConfig(String title) {
        compatItems = null;
        ConfigObject obj = Santoku.CONFIG.findPrint(currentConfig.getTitle());
        obj.setTitle(title);
        obj.values.putAll(currentConfig.values);
        currentConfig.setTitle(title);

        Santoku.CONFIG.presets.put("print", title);
        Santoku.saveConfig();
        Santoku.getCurrentConfigFile().delete();

        dropdownView.setTitle(title);
    }

    @Override
    protected void onResetConfig() {
        ConfigObject print = Santoku.CONFIG.findPrint(Santoku.CONFIG.presets.get("print"));
        if (print != null) {
            currentConfig = new ConfigObject(print);
        } else {
            currentConfig = new ConfigObject(Santoku.INSTANCE.getString(R.string.IntroCustomProfileName));
            Santoku.CONFIG.printConfigs.add(new ConfigObject(currentConfig));
            Santoku.saveConfig();
            Santoku.getCurrentConfigFile().delete();
        }
    }

    @Override
    protected ConfigObject getCurrentConfig() {
        return currentConfig;
    }

    @Override
    protected int getTitle() {
        return R.string.SlotPrintConfigTooltip;
    }

    @Override
    protected void selectItem(ProfileListItem item) {
        currentConfig = new ConfigObject((ConfigObject) item);
        Santoku.CONFIG.presets.put("print", item.getTitle());
        Santoku.saveConfig();
    }
}
