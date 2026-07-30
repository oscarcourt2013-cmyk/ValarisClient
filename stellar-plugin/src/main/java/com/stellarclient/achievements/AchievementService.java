package com.stellarclient.achievements;

import com.stellarclient.StellarClientPlugin;
import com.stellarclient.database.Database;
import com.stellarclient.detection.StellarClientDetectedEvent;
import com.stellarclient.utils.Text;
import com.stellarclient.xp.XpService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AchievementService implements Listener {

    private final StellarClientPlugin plugin;
    private final Database database;
    private final XpService xp;
    private final Map<String, AchievementDef> defs = new LinkedHashMap<>();

    public AchievementService(StellarClientPlugin plugin, Database database, XpService xp) {
        this.plugin = plugin;
        this.database = database;
        this.xp = xp;
        reload();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tickPlaytimeAchievements, 20L * 60, 20L * 60);
    }

    public void reload() {
        defs.clear();
        File file = new File(plugin.getDataFolder(), "achievements.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = cfg.getConfigurationSection("achievements");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            defs.put(id, new AchievementDef(
                    id,
                    s.getString("name", id),
                    s.getString("description", ""),
                    s.getInt("xp", 0),
                    s.getInt("playtime-hours", 0)
            ));
        }
    }

    public Map<String, AchievementDef> definitions() {
        return defs;
    }

    public List<String> unlocked(Player player) {
        return database.listAchievements(player.getUniqueId());
    }

    @EventHandler
    public void onDetected(StellarClientDetectedEvent event) {
        unlock(event.getPlayer(), "FIRST_CONNECTION");
        unlock(event.getPlayer(), "FIRST_SERVER");
    }

    public boolean unlock(Player player, String id) {
        AchievementDef def = defs.get(id);
        if (def == null) {
            return false;
        }
        if (!database.unlockAchievement(player.getUniqueId(), id, System.currentTimeMillis())) {
            return false;
        }
        if (def.xp() > 0) {
            xp.addXp(player.getUniqueId(), def.xp(), "SERVER_ACHIEVEMENT");
        }
        Text.send(player, "<gold>⚡ Succès</gold> <white>" + def.name() + "</white> <gray>— " + def.description() + "</gray>");
        return true;
    }

    private void tickPlaytimeAchievements() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!plugin.detection().isVerified(player.getUniqueId())) {
                continue;
            }
            long hours = database.getPlaytime(player.getUniqueId()) / 3600L;
            for (AchievementDef def : defs.values()) {
                if (def.playtimeHours() > 0 && hours >= def.playtimeHours()) {
                    Bukkit.getScheduler().runTask(plugin, () -> unlock(player, def.id()));
                }
            }
        }
    }

    public record AchievementDef(String id, String name, String description, int xp, int playtimeHours) {
    }
}
