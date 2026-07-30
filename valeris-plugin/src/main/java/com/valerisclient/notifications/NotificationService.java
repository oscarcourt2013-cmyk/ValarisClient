package com.valerisclient.notifications;

import com.valerisclient.ValerisClientPlugin;
import com.valerisclient.utils.Text;
import org.bukkit.entity.Player;

public final class NotificationService {

    private final ValerisClientPlugin plugin;

    public NotificationService(ValerisClientPlugin plugin) {
        this.plugin = plugin;
    }

    public void rewardAvailable(Player player, String message) {
        Text.send(player, "<gold>⚡</gold> <white>" + message + "</white>");
        if (plugin.detection().isVerified(player.getUniqueId())) {
            plugin.detection().sendNotify(player, "reward", "Valeris Reward", message, false);
        }
    }

    public void friendJoined(Player player, String friendName) {
        String template = plugin.getConfig().getString(
                "friends.message",
                "<gold>⚡</gold> <white>Votre ami <yellow>%friend%</yellow> vient de rejoindre le serveur</white>");
        String msg = template.replace("%friend%", friendName).replace("%player%", player.getName());
        Text.send(player, msg);
        if (plugin.detection().isVerified(player.getUniqueId())) {
            plugin.detection().sendNotify(player, "friend", "Friends",
                    "Votre ami " + friendName + " vient de rejoindre le serveur", true);
        }
    }

    public void event(Player player, String title, String message) {
        Text.send(player, "<gold>⚡</gold> <yellow>" + title + "</yellow> <white>" + message + "</white>");
        if (plugin.detection().isVerified(player.getUniqueId())) {
            plugin.detection().sendNotify(player, "event", title, message, true);
        }
    }
}
