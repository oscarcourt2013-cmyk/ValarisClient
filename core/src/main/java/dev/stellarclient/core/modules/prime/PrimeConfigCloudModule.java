package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.cloud.CloudSyncManager;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.profile.ProfileManager;

/** Local versioned config backups (on-disk under the game dir). */
public final class PrimeConfigCloudModule extends Module {

    private final BooleanSetting autoSync =
            addSetting(new BooleanSetting("auto-sync", "Auto backup", "Save a local backup when enabled", false));
    private final BooleanSetting uploadNow =
            addSetting(new BooleanSetting("upload", "Backup now", "Write a local backup on next tick", false));
    private final BooleanSetting downloadNow =
            addSetting(new BooleanSetting("download", "Restore now", "Restore latest local backup on next tick", false));

    private final CloudSyncManager cloudSync;
    private final ProfileManager profiles;

    public PrimeConfigCloudModule(CloudSyncManager cloudSync, ProfileManager profiles) {
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
