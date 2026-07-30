package dev.valerisclient.core.modules.valeris;

import dev.valerisclient.core.cloud.CloudSyncManager;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.profile.ProfileManager;

/** Local versioned config backups (on-disk under the game dir). */
public final class ValerisConfigCloudModule extends Module {

    private final BooleanSetting autoSync =
            addSetting(new BooleanSetting("auto-sync", "Auto backup", "Save a local backup when enabled", false));
    private final BooleanSetting uploadNow =
            addSetting(new BooleanSetting("upload", "Backup now", "Write a local backup on next tick", false));
    private final BooleanSetting downloadNow =
            addSetting(new BooleanSetting("download", "Restore now", "Restore latest local backup on next tick", false));

    private final CloudSyncManager cloudSync;
    private final ProfileManager profiles;

    public ValerisConfigCloudModule(CloudSyncManager cloudSync, ProfileManager profiles) {
        super("prime-config-cloud", "Local Config Backup", "Save and restore configs on this PC", ModuleCategory.PRIME);
        this.cloudSync = cloudSync;
        this.profiles = profiles;
        listen(ClientTickEvent.class, event -> process());
    }

    @Override
    protected void onEnable() {
        cloudSync.setAutoSync(autoSync.get());
        cloudSync.uploadNow(profiles.activeProfile());
    }

    @Override
    protected void onDisable() {
        cloudSync.setAutoSync(false);
    }

    private void process() {
        cloudSync.setAutoSync(autoSync.get() && isEnabled());
        if (uploadNow.get()) {
            uploadNow.set(false);
            cloudSync.uploadNow(profiles.activeProfile());
        }
        if (downloadNow.get()) {
            downloadNow.set(false);
            cloudSync.downloadNow(profiles.activeProfile());
        }
    }
}
