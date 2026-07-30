package dev.valerisclient.core.modules.valeris;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.bundle.ModuleBundle;
import dev.valerisclient.core.bundle.ModuleBundleRegistry;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.EnumSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.ModuleManager;
import dev.valerisclient.core.notification.NotificationManager;

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
