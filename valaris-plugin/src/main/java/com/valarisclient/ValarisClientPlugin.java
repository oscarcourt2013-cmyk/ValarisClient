package com.valarisclient;

import com.valarisclient.achievements.AchievementService;
import com.valarisclient.api.ValarisClientAPI;
import com.valarisclient.api.ValarisClientAPIImpl;
import com.valarisclient.commands.ValarisCommand;
import com.valarisclient.database.Database;
import com.valarisclient.database.DatabaseFactory;
import com.valarisclient.detection.ClientDetectionService;
import com.valarisclient.listeners.MissionListener;
import com.valarisclient.listeners.PlayerConnectionListener;
import com.valarisclient.missions.MissionService;
import com.valarisclient.notifications.NotificationService;
import com.valarisclient.placeholders.ValarisPlaceholders;
import com.valarisclient.profile.ProfileService;
import com.valarisclient.rewards.RewardService;
import com.valarisclient.xp.XpService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ValarisClientPlugin extends JavaPlugin {

    private static ValarisClientPlugin instance;
    private static ValarisClientAPI api;

    private Database database;
    private ClientDetectionService detection;
    private RewardService rewards;
    private XpService xp;
    private ProfileService profiles;
    private AchievementService achievements;
    private MissionService missions;
    private NotificationService notifications;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("achievements.yml", false);
        saveResource("missions.yml", false);

        database = DatabaseFactory.create(this);
        database.init();

        notifications = new NotificationService(this);
        detection = new ClientDetectionService(this, database);
        rewards = new RewardService(this, database, notifications);
        xp = new XpService(this, database, detection);
        achievements = new AchievementService(this, database, xp);
        missions = new MissionService(this, database, xp, notifications);
        profiles = new ProfileService(this, database, detection, xp);

        api = new ValarisClientAPIImpl(detection, database, xp);

        detection.register();
        xp.startPlaytimeTask();

        var cmd = getCommand("valaris");
        if (cmd != null) {
            ValarisCommand handler = new ValarisCommand(this, profiles, rewards, achievements);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MissionListener(missions, detection), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ValarisPlaceholders(this, detection, database).register();
            getLogger().info("PlaceholderAPI hooked.");
        }

        getLogger().info("ValarisClient enabled — channel valarisclient:main");
    }

    @Override
    public void onDisable() {
        if (xp != null) {
            xp.shutdown();
        }
        if (detection != null) {
            detection.unregister();
        }
        if (database != null) {
            database.close();
        }
        api = null;
        instance = null;
    }

    public void reloadAll() {
        reloadConfig();
        achievements.reload();
        missions.reload();
        getLogger().info("ValarisClient reloaded.");
    }

    public static ValarisClientPlugin get() {
        return instance;
    }

    public static ValarisClientAPI api() {
        return api;
    }

    public Database database() {
        return database;
    }

    public ClientDetectionService detection() {
        return detection;
    }

    public RewardService rewards() {
        return rewards;
    }

    public XpService xp() {
        return xp;
    }

    public ProfileService profiles() {
        return profiles;
    }

    public AchievementService achievements() {
        return achievements;
    }

    public MissionService missions() {
        return missions;
    }

    public NotificationService notifications() {
        return notifications;
    }
}
