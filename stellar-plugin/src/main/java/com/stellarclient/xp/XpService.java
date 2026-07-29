package com.stellarclient.xp;

import com.stellarclient.StellarClientPlugin;
import com.stellarclient.database.Database;
import com.stellarclient.detection.ClientDetectionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class XpService {

    private final StellarClientPlugin plugin;
    private final Database database;
    private final ClientDetectionService detection;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playtimeBaseline = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> sessionMinutesAwarded = new ConcurrentHashMap<>();
    private BukkitTask task;

    public XpService(StellarClientPlugin plugin, Database database, ClientDetectionService detection) {
        this.plugin = plugin;
        this.database = database;
        this.detection = detection;
    }

    public void startPlaytimeTask() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tickPlaytime, 20L * 60, 20L * 60);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            flushPlaytime(player.getUniqueId());
        }
    }

    public void onJoin(Player player) {
        UUID uuid = player.getUniqueId();
        database.upsertPlayer(uuid, player.getName());
        sessionStart.put(uuid, System.currentTimeMillis());
        playtimeBaseline.put(uuid, database.getPlaytime(uuid));
        sessionMinutesAwarded.put(uuid, 0);
    }

    public void onQuit(Player player) {
        UUID uuid = player.getUniqueId();
        flushPlaytime(uuid);
        sessionStart.remove(uuid);
        playtimeBaseline.remove(uuid);
        sessionMinutesAwarded.remove(uuid);
    }

    public void awardLogin(Player player) {
        if (!detection.isVerified(player.getUniqueId())) {
            return;
        }
        int amount = plugin.getConfig().getInt("xp.login-amount", 2);
        if (amount > 0) {
            addXp(player.getUniqueId(), amount, "SERVER_EVENT");
        }
    }

    public void addXp(UUID uuid, int amount, String reason) {
        if (uuid == null || amount == 0) {
            return;
        }
        database.addXp(uuid, amount);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && detection.isVerified(uuid)
                && plugin.getConfig().getBoolean("xp.notify-client", true)) {
            String kind = reason == null ? "SERVER_EVENT" : reason;
            Bukkit.getScheduler().runTask(plugin, () -> detection.sendXpPacket(player, amount, kind));
        }
    }

    private void tickPlaytime() {
        int every = Math.max(1, plugin.getConfig().getInt("xp.playtime-minutes", 10));
        int amount = plugin.getConfig().getInt("xp.playtime-amount", 5);
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (!detection.isVerified(uuid)) {
                continue;
            }
            Long start = sessionStart.get(uuid);
            if (start == null) {
                continue;
            }
            int minutes = (int) ((now - start) / 60_000L);
            int awarded = sessionMinutesAwarded.getOrDefault(uuid, 0);
            int chunks = minutes / every;
            int already = awarded / every;
            int grant = (chunks - already) * amount;
            if (grant > 0) {
                sessionMinutesAwarded.put(uuid, chunks * every);
                addXp(uuid, grant, "SERVER_PLAYTIME");
            }
            flushPlaytime(uuid);
        }
    }

    private void flushPlaytime(UUID uuid) {
        Long start = sessionStart.get(uuid);
        Long baseline = playtimeBaseline.get(uuid);
        if (start == null || baseline == null) {
            return;
        }
        long sessionSecs = Math.max(0, (System.currentTimeMillis() - start) / 1000L);
        database.setPlaytime(uuid, baseline + sessionSecs);
    }

    public String formatHours(long seconds) {
        return String.format("%.1f", seconds / 3600.0);
    }
}
