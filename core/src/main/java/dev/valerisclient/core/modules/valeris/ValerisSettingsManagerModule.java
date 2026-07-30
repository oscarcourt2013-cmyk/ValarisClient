package dev.valerisclient.core.modules.valeris;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.ModuleManager;
import dev.valerisclient.core.module.Setting;

/** Opens the ClickGUI and optionally resets all module settings. */
public final class ValerisSettingsManagerModule extends Module {

    private final BooleanSetting resetOnDisable =
            addSetting(new BooleanSetting("reset-on-disable", "Reset on disable", "Reset all modules when turned off", false));

    private final ModuleManager modules;
    private final MinecraftAdapter adapter;

    public ValerisSettingsManagerModule(ModuleManager modules, MinecraftAdapter adapter) {
        super("prime-settings-manager", "Valeris Settings Manager", "Opens the ClickGUI for module settings", ModuleCategory.PRIME);
        this.modules = modules;
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        adapter.openClickGui();
        setEnabled(false);
    }

    @Override
    protected void onDisable() {
        if (!resetOnDisable.get()) {
            return;
        }
        for (Module module : modules.all()) {
            for (Setting setting : module.settings()) {
                setting.reset();
            }
            module.setEnabled(false);
        }
    }
}
