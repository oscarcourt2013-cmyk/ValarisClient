package com.valarisclient.notifications;

import com.valarisclient.ValarisClientPlugin;
import com.valarisclient.utils.Text;
import org.bukkit.entity.Player;

public final class NotificationService {

    private final ValarisClientPlugin plugin;

    public NotificationService(ValarisClientPlugin plugin) {
        this.plugin = plugin;
    }

    public void rewardAvailable(Player player, String message) {
        Text.send(player, "<gold>⚡</gold> <white>" + message + "</white>");
        if (plugin.detection().isVerified(player.getUniqueId())) {
            plugin.detection().sendNotify(player, "reward", "Valaris Reward", message, false);
        }
    }

    public void event(Player player, String title, String message) {
        Text.send(player, "<gold>⚡</gold> <yellow>" + title + "</yellow> <white>" + message + "</white>");
        if (plugin.detection().isVerified(player.getUniqueId())) {
            plugin.detection().sendNotify(player, "event", title, message, true);
        }
    }
}
