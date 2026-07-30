package com.stellarclient;

import com.stellarclient.achievements.AchievementService;
import com.stellarclient.api.StellarClientAPI;
import com.stellarclient.api.StellarClientAPIImpl;
import com.stellarclient.commands.PrimeCommand;
import com.stellarclient.database.Database;
import com.stellarclient.database.DatabaseFactory;
import com.stellarclient.detection.ClientDetectionService;
import com.stellarclient.friends.FriendsService;
import com.stellarclient.listeners.MissionListener;
import com.stellarclient.listeners.PlayerConnectionListener;
import com.stellarclient.missions.MissionService;
import com.stellarclient.network.NetworkService;
import com.stellarclient.notifications.NotificationService;
import com.stellarclient.placeholders.PrimePlaceholders;
import com.stellarclient.profile.ProfileService;
import com.stellarclient.rewards.RewardService;
import com.stellarclient.xp.XpService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class StellarClientPlugin extends JavaPlugin {

    private static StellarClientPlugin instance;
    private static StellarClientAPI api;

    private Database database;
    private ClientDetectionService detection;
    private RewardService rewards;
    private XpService xp;
    private ProfileService profiles;
    private AchievementService achievements;
    private MissionService missions;
    private FriendsService friends;
    private NetworkService network;
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
        friends = new FriendsService(this, notifications);
        network = new NetworkService(this);

        api = new StellarClientAPIImpl(detection, database, xp);

        detection.register();
        xp.startPlaytimeTask();
        network.start();

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

        getLogger().info("StellarClient enabled — channel stellarclient:main");
    }

    @Override
    public void onDisable() {
        if (xp != null) {
            xp.shutdown();
        }
        if (network != null) {
            network.shutdown();
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
        getLogger().info("StellarClient reloaded.");
    }

    public static StellarClientPlugin get() {
        return instance;
    }

    public static StellarClientAPI api() {
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

    public FriendsService friends() {
        return friends;
    }

    public NetworkService network() {
        return network;
    }

    public NotificationService notifications() {
        return notifications;
    }
}
