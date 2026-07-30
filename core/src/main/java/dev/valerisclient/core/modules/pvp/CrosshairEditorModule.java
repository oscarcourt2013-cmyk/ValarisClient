package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.crosshair.CrosshairConfig;
import dev.valerisclient.core.crosshair.CrosshairPresetName;
import dev.valerisclient.core.crosshair.CrosshairPresetStore;
import dev.valerisclient.core.crosshair.CrosshairProfileManager;
import dev.valerisclient.core.crosshair.CrosshairRenderer;
import dev.valerisclient.core.crosshair.CrosshairStyle;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.ColorSetting;
import dev.valerisclient.core.module.DoubleSetting;
import dev.valerisclient.core.module.EnumSetting;
import dev.valerisclient.core.module.IntSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.state.CrosshairState;
import dev.valerisclient.core.theme.ThemeManager;

/** Full crosshair editor with styles, presets and live preview. */
public final class CrosshairEditorModule extends Module {

    private final IntSetting size = addSetting(new IntSetting("size", "Size", "Crosshair box size", 11, 5, 32));
    private final IntSetting armLength = addSetting(new IntSetting("arm", "Arm length", "Arm length in pixels", 4, 1, 16));
    private final IntSetting thickness = addSetting(new IntSetting("thick", "Thickness", "Line thickness", 1, 1, 4));
    private final IntSetting gap = addSetting(new IntSetting("gap", "Gap", "Center gap", 2, 0, 12));
    private final ColorSetting color = addSetting(new ColorSetting("color", "Color", "Crosshair color", 0xFFFFFFFF));
    private final DoubleSetting opacity = addSetting(new DoubleSetting("opacity", "Opacity", "Crosshair opacity", 1.0, 0.1, 1.0));
    private final DoubleSetting rotation = addSetting(new DoubleSetting("rotation", "Rotation", "Rotation in degrees", 0.0, 0.0, 360.0));
    private final EnumSetting<CrosshairStyle> style =
            addSetting(new EnumSetting<>("style", "Style", "Crosshair style", CrosshairStyle.CLASSIC));
    private final EnumSetting<CrosshairPresetName> preset =
            addSetting(new EnumSetting<>("preset", "Preset", "Load a saved preset", CrosshairPresetName.Classic));
    private final StringSetting presetName =
            addSetting(new StringSetting("preset-name", "Preset name", "Name for export/import", "Custom"));
    private final BooleanSetting exportPreset =
            addSetting(new BooleanSetting("export", "Export preset", "Save current crosshair as preset", false));
    private final BooleanSetting importPreset =
            addSetting(new BooleanSetting("import", "Import preset", "Load preset by name", false));
    private final BooleanSetting saveServerProfile =
            addSetting(new BooleanSetting("save-profile", "Save server profile", "Store for current server", false));

    private final MinecraftAdapter adapter;
    private final CrosshairConfig config;
    private final CrosshairPresetStore presets;
    private final CrosshairProfileManager profiles;
    private final CrosshairElement crosshair;
    private String lastPreset = "";

    public CrosshairEditorModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter,
                                 CrosshairConfig config, CrosshairPresetStore presets,
                                 CrosshairProfileManager profiles) {
        super("crosshair-editor", "Crosshair Editor", "Premium customizable crosshair", ModuleCategory.PVP);
        this.adapter = adapter;
        this.config = config;
        this.presets = presets;
        this.profiles = profiles;
        this.crosshair = hud.register(new CrosshairElement(config));

        listen(ClientTickEvent.class, event -> syncConfig());
    }

    @Override
    protected void onEnable() {
        syncConfig();
        crosshair.setVisible(true);
        CrosshairState.setActive(config, true);
    }

    @Override
    protected void onDisable() {
        crosshair.setVisible(false);
        CrosshairState.setActive(config, false);
    }

    private void syncConfig() {
        if (!preset.get().name().equals(lastPreset)) {
            presets.applyTo(config, preset.get().name());
            lastPreset = preset.get().name();
            pullFromConfig();
        }
        if (exportPreset.get()) {
            pushToConfig();
            presets.save(presetName.get(), config);
            exportPreset.set(false);
        }
        if (importPreset.get()) {
            presets.applyTo(config, presetName.get());
            pullFromConfig();
            importPreset.set(false);
        }
        if (saveServerProfile.get()) {
            pushToConfig();
            profiles.saveCurrentForServer(adapter.serverAddress());
            saveServerProfile.set(false);
        }
        pushToConfig();
        if (isEnabled()) {
            CrosshairState.setActive(config, true);
        }
    }

    private void pushToConfig() {
        config.size = size.get();
        config.armLength = armLength.get();
        config.thickness = thickness.get();
        config.gap = gap.get();
        config.color = color.get();
        config.opacity = (float) opacity.get();
        config.rotation = (float) rotation.get();
        config.style = style.get();
        config.serverProfile = adapterProfile();
    }

    private void pullFromConfig() {
        size.set(config.size);
        armLength.set(config.armLength);
        thickness.set(config.thickness);
        gap.set(config.gap);
        color.set(config.color);
        opacity.set(config.opacity);
        rotation.set(config.rotation);
        style.set(config.style);
    }

    private String adapterProfile() {
        String server = adapter.serverAddress();
        if (server == null || server.isBlank()) {
            return "global";
        }
        return server;
    }

    private static final class CrosshairElement extends HudElement {
        private final CrosshairConfig config;

        CrosshairElement(CrosshairConfig config) {
            super("crosshair", "Crosshair", HudAnchor.CENTER, 0, 0);
            this.config = config;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return config.size;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return config.size;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            CrosshairRenderer.render(ctx, config);
        }
    }
}
