package com.valerisclient;

import com.valerisclient.achievements.AchievementService;
import com.valerisclient.api.ValerisClientAPI;
import com.valerisclient.api.ValerisClientAPIImpl;
import com.valerisclient.commands.PrimeCommand;
import com.valerisclient.database.Database;
import com.valerisclient.database.DatabaseFactory;
import com.valerisclient.detection.ClientDetectionService;
import com.valerisclient.listeners.MissionListener;
import com.valerisclient.listeners.PlayerConnectionListener;
import com.valerisclient.missions.MissionService;
import com.valerisclient.notifications.NotificationService;
import com.valerisclient.placeholders.PrimePlaceholders;
import com.valerisclient.profile.ProfileService;
import com.valerisclient.rewards.RewardService;
import com.valerisclient.xp.XpService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ValerisClientPlugin extends JavaPlugin {

    private static ValerisClientPlugin instance;
    private static ValerisClientAPI api;

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

        api = new ValerisClientAPIImpl(detection, database, xp);

        detection.register();
        xp.startPlaytimeTask();

        var cmd = getCommand("prime");
        if (cmd != null) {
            PrimeCommand handler = new PrimeCommand(this, profiles, rewards, achievements);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MissionListener(missions, detection), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PrimePlaceholders(this, detection, database).register();
            getLogger().info("PlaceholderAPI hooked.");
        }

        getLogger().info("ValerisClient enabled — channel valerisclient:main");
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
        getLogger().info("ValerisClient reloaded.");
    }

    public static ValerisClientPlugin get() {
        return instance;
    }

    public static ValerisClientAPI api() {
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
