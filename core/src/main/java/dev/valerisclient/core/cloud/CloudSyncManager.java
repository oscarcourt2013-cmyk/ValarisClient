package dev.valerisclient.core.cloud;

import com.google.gson.JsonElement;
import dev.valerisclient.core.config.ConfigManager;
import dev.valerisclient.core.notification.NotificationManager;

/** Orchestrates auto-sync and version restore via {@link CloudClient}. */
public final class CloudSyncManager {

    private final CloudClient cloud;
    private final ConfigManager configManager;
    private final NotificationManager notifications;
    private boolean autoSync;
    private long lastSyncMillis;

    public CloudSyncManager(CloudClient cloud, ConfigManager configManager, NotificationManager notifications) {
        this.cloud = cloud;
        this.configManager = configManager;
        this.notifications = notifications;
    }

    public boolean autoSync() {
        return autoSync;
    }

    public void setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
    }

    public void uploadNow(String profileName) {
        JsonElement snapshot = configManager.exportAll();
        cloud.uploadConfig(profileName, snapshot);
        lastSyncMillis = System.currentTimeMillis();
        notifications.success("Local backup", "Configuration saved on this PC");
    }

    public boolean downloadNow(String profileName) {
        return cloud.downloadConfig(profileName).map(json -> {
            configManager.importAll(json);
            lastSyncMillis = System.currentTimeMillis();
            notifications.success("Local backup", "Configuration restored from this PC");
            return true;
        }).orElse(false);
    }

    public boolean restoreVersion(String profileName, String versionId) {
        return cloud.restoreVersion(profileName, versionId).map(json -> {
            configManager.importAll(json);
            notifications.success("Local backup", "Configuration version restored");
            return true;
        }).orElse(false);
    }

    public long lastSyncMillis() {
        return lastSyncMillis;
    }

    public java.util.List<CloudClient.VersionEntry> listVersions(String profileName) {
        return cloud.listVersions(profileName);
    }

    public CloudClient cloudClient() {
        return cloud;
    }
}
