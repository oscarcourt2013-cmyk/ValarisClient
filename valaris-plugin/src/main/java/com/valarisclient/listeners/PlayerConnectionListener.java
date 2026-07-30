package com.valarisclient.listeners;

import com.valarisclient.ValarisClientPlugin;
import com.valarisclient.detection.ValarisClientDetectedEvent;
import com.valarisclient.utils.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class PlayerConnectionListener implements Listener {

    private final ValarisClientPlugin plugin;

    public PlayerConnectionListener(ValarisClientPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.database().upsertPlayer(player.getUniqueId(), player.getName());
        plugin.xp().onJoin(player);
        plugin.missions().onJoin(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.xp().onQuit(player);
        plugin.detection().clear(player.getUniqueId());
        clearTab(player);
    }

    @EventHandler
    public void onDetected(ValarisClientDetectedEvent event) {
        Player player = event.getPlayer();
        plugin.xp().awardLogin(player);
        applyTab(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        boolean valaris = plugin.detection().isVerified(player.getUniqueId())
                || player.hasPermission("ValarisClient.verified");
        if (!valaris) {
            return;
        }
        if (plugin.getConfig().getBoolean("verified.require-detection", true)
                && !plugin.detection().isVerified(player.getUniqueId())
                && !player.hasPermission("ValarisClient.verified")) {
            return;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());
        String format = plugin.getConfig().getString(
                "verified.chat-format",
                "<gold>⚡</gold> <gray>%player%</gray> <dark_gray>»</dark_gray> <white>%message%</white>");
        event.renderer((source, sourceDisplayName, message, viewer) ->
                Text.mm(format
                        .replace("%player%", player.getName())
                        .replace("%message%", plain)));
    }

    private void applyTab(Player player) {
        String prefix = plugin.getConfig().getString("verified.tab-prefix", "<gold>⚡ </gold>");
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = ("pc_" + player.getUniqueId().toString().replace("-", "")).substring(0, 16);
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.prefix(Text.mm(prefix));
        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
        player.setPlayerListName(null);
    }

    private void clearTab(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = ("pc_" + player.getUniqueId().toString().replace("-", "")).substring(0, 16);
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }
}
