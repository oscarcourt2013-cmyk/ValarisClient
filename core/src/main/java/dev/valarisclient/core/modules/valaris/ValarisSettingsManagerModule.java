package dev.valarisclient.core.modules.valaris;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.module.BooleanSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.module.ModuleManager;
import dev.valarisclient.core.module.Setting;

/** Opens the ClickGUI and optionally resets all module settings. */
public final class ValarisSettingsManagerModule extends Module {

    private final BooleanSetting resetOnDisable =
            addSetting(new BooleanSetting("reset-on-disable", "Reset on disable", "Reset all modules when turned off", false));

    private final ModuleManager modules;
    private final MinecraftAdapter adapter;

    public ValarisSettingsManagerModule(ModuleManager modules, MinecraftAdapter adapter) {
        super("valaris-settings-manager", "Valaris Settings Manager", "Opens the ClickGUI for module settings", ModuleCategory.VALARIS);
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
