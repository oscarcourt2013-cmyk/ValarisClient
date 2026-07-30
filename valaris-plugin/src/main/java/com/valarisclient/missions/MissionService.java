package com.valarisclient.missions;

import com.valarisclient.ValarisClientPlugin;
import com.valarisclient.database.Database;
import com.valarisclient.notifications.NotificationService;
import com.valarisclient.utils.Text;
import com.valarisclient.xp.XpService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MissionService {

    private final ValarisClientPlugin plugin;
    private final Database database;
    private final XpService xp;
    private final NotificationService notifications;
    private final Map<String, MissionDef> defs = new LinkedHashMap<>();

    public MissionService(
            ValarisClientPlugin plugin,
            Database database,
            XpService xp,
            NotificationService notifications) {
        this.plugin = plugin;
        this.database = database;
        this.xp = xp;
        this.notifications = notifications;
        reload();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tickPlaytimeMissions, 20L * 60, 20L * 60);
    }

    public void reload() {
        defs.clear();
        File file = new File(plugin.getDataFolder(), "missions.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("missions");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            ConfigurationSection rewards = s.getConfigurationSection("rewards");
            defs.put(id, new MissionDef(
                    id,
                    s.getString("name", id),
                    s.getString("description", ""),
                    s.getString("type", "playtime"),
                    s.getInt("target", 1),
                    s.getString("reset", "never"),
                    rewards == null ? 0 : rewards.getInt("xp", 0),
                    rewards == null ? List.of() : rewards.getStringList("commands"),
                    rewards == null ? "" : rewards.getString("message", "")
            ));
        }
    }

    public void onJoin(Player player) {
        progress(player, "join_server", 1);
    }

    public void onBlockBreak(Player player) {
        progress(player, "break_blocks", 1);
    }

    public void onPlayerKill(Player player) {
        progress(player, "kill_players", 1);
    }

    public void progress(Player player, String type, int amount) {
        if (!plugin.detection().isVerified(player.getUniqueId())) {
            return;
        }
        for (MissionDef def : defs.values()) {
            if (!def.type().equalsIgnoreCase(type)) {
                continue;
            }
            if (database.isMissionComplete(player.getUniqueId(), def.id())) {
                continue;
            }
            int next = database.getMissionProgress(player.getUniqueId(), def.id()) + amount;
            database.setMissionProgress(player.getUniqueId(), def.id(), next);
            if (next >= def.target()) {
                complete(player, def);
            }
        }
    }

    private void tickPlaytimeMissions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.detection().isVerified(player.getUniqueId())) {
                continue;
            }
            // Approximate: award 1 minute progress each tick for playtime missions
            Bukkit.getScheduler().runTask(plugin, () -> progress(player, "playtime", 1));
        }
    }

    private void complete(Player player, MissionDef def) {
        database.completeMission(player.getUniqueId(), def.id(), System.currentTimeMillis());
        if (def.rewardXp() > 0) {
            xp.addXp(player.getUniqueId(), def.rewardXp(), "SERVER_EVENT");
        }
        for (String cmd : def.rewardCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Text.applyPlayer(cmd, player));
        }
        if (def.rewardMessage() != null && !def.rewardMessage().isBlank()) {
            Text.send(player, def.rewardMessage());
        } else {
            notifications.event(player, "Mission", def.name() + " accomplie");
        }
    }

    public record MissionDef(
            String id,
            String name,
            String description,
            String type,
            int target,
            String reset,
            int rewardXp,
            List<String> rewardCommands,
            String rewardMessage
    ) {
    }
}
