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
import com.dark98.santoku.slic3r.Slic3rLocalization;
import com.dark98.santoku.slic3r.Slic3rUtils;
import com.dark98.santoku.utils.ViewUtils;

public class FilamentConfigFragment extends ProfileListFragment {
    private List<ProfileListItem> compatItems;
    private String lastPrinter;
    private String lastPrint;
    private int lastUid;
    private ConfigObject currentConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        onResetConfig();
    }

    @Override
    protected List<ProfileListItem> getItems(boolean filter) {
        List<ConfigObject> list = Santoku.CONFIG.filamentConfigs;
        if (filter) {
            String printer = Santoku.CONFIG.presets.get("printer");
            String print = Santoku.CONFIG.presets.get("print");
            if (Objects.equals(lastPrinter, printer) && Objects.equals(lastPrint, print) && compatItems != null && lastUid == Santoku.CONFIG_UID) {
                return compatItems;
            }

            List<ConfigObject> nList = new ArrayList<>(list.size());
            Slic3rUtils.ConfigChecker checker = new Slic3rUtils.ConfigChecker(Santoku.CONFIG.findPrinter(printer).serialize());
            if (Santoku.CONFIG.findPrint(print) != null) {
                Slic3rUtils.ConfigChecker printChecker = new Slic3rUtils.ConfigChecker(Santoku.CONFIG.findPrint(print).serialize());
                for (ConfigObject obj : list) {
                    if (checker.checkCompatibility(obj.get("compatible_printers_condition")) && printChecker.checkCompatibility(obj.get("compatible_prints_condition"))) {
                        nList.add(obj);
                    }
                }
                printChecker.release();
            }
            checker.release();
            lastPrinter = printer;
            lastPrint = print;
            lastUid = Santoku.CONFIG_UID;
            return compatItems = (List) nList;
        }
        return (List) list;
    }

    @Override
    protected List<OptionElement> getConfigItems() {
        PrintConfigDef def = PrintConfigDef.getInstance();
        return Arrays.asList(
                new OptionElement(R.drawable.slot_filament_28, Slic3rLocalization.getString("Filament")),
                new OptionElement(new SubHeader("Filament")),
                new OptionElement(def.options.get("filament_colour")),
                new OptionElement(def.options.get("filament_diameter")),
                new OptionElement(def.options.get("extrusion_multiplier")),
                new OptionElement(def.options.get("filament_density")),
                new OptionElement(def.options.get("filament_cost")),
                new OptionElement(def.options.get("filament_spool_weight")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Temperature")),
                new OptionElement(def.options.get("idle_temperature")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Nozzle")),
                new OptionElement(def.options.get("first_layer_temperature")),
                new OptionElement(def.options.get("temperature")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Bed")),
                new OptionElement(def.options.get("first_layer_bed_temperature")),
                new OptionElement(def.options.get("bed_temperature")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Chamber")),
                new OptionElement(def.options.get("chamber_temperature")),
                new OptionElement(def.options.get("chamber_minimal_temperature")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(R.drawable.mode_fan_24, Slic3rLocalization.getString("Cooling")),
                new OptionElement(new SubHeader("Enable")),
                new OptionElement(def.options.get("fan_always_on")),
                new OptionElement(def.options.get("cooling")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Fan speed")),
                new OptionElement(def.options.get("min_fan_speed")),
                new OptionElement(def.options.get("max_fan_speed")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(def.options.get("bridge_fan_speed")),
                new OptionElement(def.options.get("disable_fan_first_layers")),
                new OptionElement(def.options.get("full_fan_speed_layer")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Dynamic fan speeds")),
                new OptionElement(def.options.get("enable_dynamic_fan_speeds")),
                new OptionElement(def.options.get("overhang_fan_speed_0")),
                new OptionElement(def.options.get("overhang_fan_speed_1")),
                new OptionElement(def.options.get("overhang_fan_speed_2")),
                new OptionElement(def.options.get("overhang_fan_speed_3")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Cooling thresholds")),
                new OptionElement(def.options.get("fan_below_layer_time")),
                new OptionElement(def.options.get("slowdown_below_layer_time")),
                new OptionElement(def.options.get("min_print_speed")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(R.drawable.settings_outline_28, Slic3rLocalization.getString("Advanced")),
                new OptionElement(new SubHeader("Filament properties")),
                new OptionElement(def.options.get("filament_type")),
                new OptionElement(def.options.get("filament_soluble")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Print speed override")),
                new OptionElement(def.options.get("filament_max_volumetric_speed")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(def.options.get("filament_infill_max_speed")),
                new OptionElement(def.options.get("filament_infill_max_crossing_speed")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Shrinkage compensation")),
                // TODO: Support x_size_compensation/y_size_compensation instead (I'm too lazy to write description for now)
//                new OptionElement(def.options.get("filament_shrinkage_compensation_x")),
//                new OptionElement(def.options.get("filament_shrinkage_compensation_y")),
                new OptionElement(def.options.get("filament_shrinkage_compensation_xy")),
                new OptionElement(def.options.get("filament_shrinkage_compensation_z")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Wipe tower parameters")),
                new OptionElement(def.options.get("filament_minimal_purge_on_wipe_tower")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Toolchange parameters with single extruder MM printers")),
                new OptionElement(def.options.get("filament_loading_speed_start")),
                new OptionElement(def.options.get("filament_loading_speed")),
                new OptionElement(def.options.get("filament_unloading_speed_start")),
                new OptionElement(def.options.get("filament_unloading_speed")),
                new OptionElement(def.options.get("filament_load_time")),
                new OptionElement(def.options.get("filament_unload_time")),
                new OptionElement(def.options.get("filament_toolchange_delay")),
                new OptionElement(def.options.get("filament_cooling_moves")),
                new OptionElement(def.options.get("filament_cooling_initial_speed")),
                new OptionElement(def.options.get("filament_cooling_final_speed")),
                new OptionElement(def.options.get("filament_stamping_loading_speed")),
                new OptionElement(def.options.get("filament_stamping_distance")),
                new OptionElement(def.options.get("filament_purge_multiplier")),
                new OptionElement(def.options.get("filament_ramming_parameters")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Toolchange parameters with multi extruder MM printers")),
                new OptionElement(def.options.get("filament_multitool_ramming")),
                new OptionElement(def.options.get("filament_multitool_ramming_volume")),
                new OptionElement(def.options.get("filament_multitool_ramming_flow")),

                new OptionElement(R.drawable.settings_outline_28, Slic3rLocalization.getString("Filament Overrides")),
                new OptionElement(new SubHeader("Travel lift")),
                new OptionElement(def.options.get("filament_retract_lift")),
                new OptionElement(def.options.get("filament_travel_ramping_lift")),
                new OptionElement(def.options.get("filament_travel_max_lift")),
                new OptionElement(def.options.get("filament_travel_slope")),
                new OptionElement(def.options.get("filament_travel_lift_before_obstacle")),
                new OptionElement(def.options.get("filament_retract_lift_above")),
                new OptionElement(def.options.get("filament_retract_lift_below")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Retraction")),
                new OptionElement(def.options.get("filament_retract_length")),
                new OptionElement(def.options.get("filament_retract_speed")),
                new OptionElement(def.options.get("filament_deretract_speed")),
                new OptionElement(def.options.get("filament_retract_restart_extra")),
                new OptionElement(def.options.get("filament_retract_before_travel")),
                new OptionElement(def.options.get("filament_retract_layer_change")),
                new OptionElement(def.options.get("filament_wipe")),
                new OptionElement(def.options.get("filament_retract_before_wipe")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("Retraction when tool is disabled")),
                new OptionElement(def.options.get("filament_retract_length_toolchange")),
                new OptionElement(def.options.get("filament_retract_restart_extra_toolchange")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(R.drawable.settings_outline_28, Slic3rLocalization.getString("Custom G-code")),
                new OptionElement(new SubHeader("Start G-code")),
                new OptionElement(def.options.get("start_filament_gcode")),
                new OptionElement(new SpaceItem(0, ViewUtils.dp(4))),

                new OptionElement(new SubHeader("End G-code")),
                new OptionElement(def.options.get("end_filament_gcode")),

                new OptionElement(R.drawable.note_pen_outline_96, Slic3rLocalization.getString("Notes")),
                new OptionElement(new SubHeader("Notes")),
                new OptionElement(def.options.get("filament_notes")),

                new OptionElement(R.drawable.wrench_outline_28, Slic3rLocalization.getString("Dependencies")),
                new OptionElement(new SubHeader("Profile dependencies")),
                new OptionElement(def.options.get("compatible_printers_condition")),
                new OptionElement(def.options.get("compatible_prints")),
                new OptionElement(def.options.get("compatible_prints_condition"))

        );
    }

    @Override
    protected void cloneCurrentProfile() {
        ConfigObject obj = new ConfigObject(Santoku.INSTANCE.getString(R.string.SettingsProfileCopy, currentConfig.getTitle()));
        obj.values.putAll(currentConfig.values);
        currentConfig = new ConfigObject(obj);

        Santoku.CONFIG.filamentConfigs.add(obj);
        Santoku.CONFIG.presets.put("filament", obj.getTitle());
        Santoku.saveConfig();
        Santoku.getCurrentConfigFile().delete();

        currentConfig = new ConfigObject(obj);
        dropdownView.setTitle(getCurrentConfig().getTitle());
        compatItems = null;
    }

    @Override
    protected void deleteCurrentProfile() {
        compatItems = null;
        Santoku.CONFIG.filamentConfigs.remove(Santoku.CONFIG.findFilament(currentConfig.getTitle()));
        selectItem(getItems(true).get(0));

        dropdownView.setTitle(getCurrentConfig().getTitle());
    }

    @Override
    protected void onApplyConfig(String title) {
        compatItems = null;
        ConfigObject obj = Santoku.CONFIG.findFilament(currentConfig.getTitle());
        obj.setTitle(title);
        obj.values.putAll(currentConfig.values);
        currentConfig.setTitle(title);

        Santoku.CONFIG.presets.put("filament", title);
        Santoku.saveConfig();
        Santoku.getCurrentConfigFile().delete();

        dropdownView.setTitle(title);
    }

    @Override
    protected void onResetConfig() {
        currentConfig = new ConfigObject(Santoku.CONFIG.findFilament(Santoku.CONFIG.presets.get("filament")));
    }

    @Override
    protected ConfigObject getCurrentConfig() {
        return currentConfig;
    }

    @Override
    protected int getTitle() {
        return R.string.SlotFilamentConfigTooltip;
    }

    @Override
    protected void selectItem(ProfileListItem item) {
        currentConfig = new ConfigObject((ConfigObject) item);
        Santoku.CONFIG.presets.put("filament", item.getTitle());
        Santoku.saveConfig();
    }
}
