package dev.stellarclient.core.modules.streamers;

import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;

/** Master toggle that enables the full stream-privacy stack in one click. */
public final class StreamPrivacySuiteModule extends Module {

    private static final String[] SIBLING_IDS = {
            "stream-debug-shield",
            "stream-chat-redact",
            "stream-name-mask",
            "stream-hud-shield",
            "stream-branding"
    };

    private final BooleanSetting autoEnableAll =
            addSetting(new BooleanSetting(
                    "auto-enable-all",
                    "Auto-enable all",
                    "Enable every stream privacy module when this turns on",
                    true));
    private final BooleanSetting disableSiblings =
            addSetting(new BooleanSetting(
                    "disable-siblings",
                    "Disable siblings",
                    "Turn off stream privacy modules when this turns off",
                    true));

    private final ModuleManager modules;

    public StreamPrivacySuiteModule(ModuleManager modules) {
        super("stream-privacy-suite", "Stream Privacy Suite",
                "One-click stream privacy for coordinates, chat, names, and HUD", ModuleCategory.STREAMERS);
        this.modules = modules;
    }

    @Override
    protected void onEnable() {
        if (autoEnableAll.get()) {
            for (String id : SIBLING_IDS) {
                Module module = modules.get(id);
                if (module != null && !module.isEnabled()) {
                    module.setEnabled(true);
                }
            }
        }
    }

    @Override
    protected void onDisable() {
        if (disableSiblings.get()) {
            for (String id : SIBLING_IDS) {
                Module module = modules.get(id);
                if (module != null && module.isEnabled()) {
                    module.setEnabled(false);
                }
            }
        }
    }
}
