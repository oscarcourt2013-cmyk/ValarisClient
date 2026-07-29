package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.module.Setting;

/** Opens the ClickGUI and optionally resets all module settings. */
public final class PrimeSettingsManagerModule extends Module {

    private final BooleanSetting resetOnDisable =
            addSetting(new BooleanSetting("reset-on-disable", "Reset on disable", "Reset all modules when turned off", false));

    private final ModuleManager modules;
    private final MinecraftAdapter adapter;

    public PrimeSettingsManagerModule(ModuleManager modules, MinecraftAdapter adapter) {
        super("prime-settings-manager", "Prime Settings Manager", "Opens the ClickGUI for module settings", ModuleCategory.PRIME);
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
