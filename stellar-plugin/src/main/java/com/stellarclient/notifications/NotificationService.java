package com.stellarclient.notifications;

import com.stellarclient.StellarClientPlugin;
import com.stellarclient.utils.Text;
import org.bukkit.entity.Player;

public final class NotificationService {

    private final StellarClientPlugin plugin;

    public NotificationService(StellarClientPlugin plugin) {
        this.plugin = plugin;
    }

    public void rewardAvailable(Player player, String message) {
        Text.send(player, "<gold>âš¡</gold> <white>" + message + "</white>");
        if (plugin.detection().isVerified(player.getUniqueId())) {
            plugin.detection().sendNotify(player, "reward", "Prime Reward", message, false);
        }
    }

    public void friendJoined(Player player, String friendName) {
        String template = plugin.getConfig().getString(
                "friends.message",
                "<gold>âš¡</gold> <white>Votre ami <yellow>%friend%</yellow> vient de rejoindre le serveur</white>");
        String msg = template.replace("%friend%", friendName).replace("%player%", player.getName());
        Text.send(player, msg);
        if (plugin.detection().isVerified(player.getUniqueId())) {
            plugin.detection().sendNotify(player, "friend", "Friends",
                    "Votre ami " + friendName + " vient de rejoindre le serveur", true);
        }
    }

    public void event(Player player, String title, String message) {
        Text.send(player, "<gold>âš¡</gold> <yellow>" + title + "</yellow> <white>" + message + "</white>");
        if (plugin.detection().isVerified(player.getUniqueId())) {
            plugin.detection().sendNotify(player, "event", title, message, true);
        }
    }
}
