package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.bundle.ModuleBundle;
import dev.stellarclient.core.bundle.ModuleBundleRegistry;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.EnumSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.notification.NotificationManager;

/** One-click enable for module bundle presets. */
public final class ModuleBundlesModule extends Module {

    public enum Bundle {
        CPVP(ModuleBundleRegistry.CPVP_KIT),
        HARDCORE(ModuleBundleRegistry.HARDCORE_SURVIVAL),
        SPEEDRUN(ModuleBundleRegistry.SPEEDRUN_LITE),
        STREAMER(ModuleBundleRegistry.STREAMER_PACK);

        private final ModuleBundle bundle;

        Bundle(ModuleBundle bundle) {
            this.bundle = bundle;
        }

        ModuleBundle bundle() {
            return bundle;
        }
    }

    private final EnumSetting<Bundle> bundleSetting =
            addSetting(new EnumSetting<>("bundle", "Bundle", "Preset to apply", Bundle.CPVP));

    private final ModuleManager modules;
    private final NotificationManager notifications;
    private Bundle lastApplied = Bundle.CPVP;

    public ModuleBundlesModule(ModuleManager modules, NotificationManager notifications) {
        super("module-bundles", "Module Bundles", "One-click CPvP, Survival, Speedrun, Streamer packs", ModuleCategory.PRIME);
        this.modules = modules;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> applyIfChanged());
    }

    private void applyIfChanged() {
        Bundle selected = bundleSetting.get();
        if (selected == lastApplied) {
            return;
        }
        applyBundle(selected.bundle());
        lastApplied = selected;
    }

    /** Applies a bundle immediately (also used by FirstRunConfigurator). */
    public static void applyBundle(ModuleManager modules, ModuleBundle bundle, NotificationManager notifications) {
        for (String id : bundle.moduleIds()) {
            Module module = modules.get(id);
            if (module != null && !module.isEnabled()) {
                module.setEnabled(true);
            }
        }
        if (notifications != null) {
            notifications.success("Module Bundles", bundle.name() + " enabled");
        }
    }

    private void applyBundle(ModuleBundle bundle) {
        applyBundle(modules, bundle, notifications);
    }
}
