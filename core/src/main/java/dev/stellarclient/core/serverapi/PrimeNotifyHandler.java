package dev.stellarclient.core.serverapi;

import com.google.gson.JsonObject;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.notification.NotificationManager;

/** Handles {@code NOTIFY} packets (friend / reward / event / announce). */
public final class PrimeNotifyHandler {

    private final NotificationManager notifications;
    private final MinecraftAdapter adapter;

    public PrimeNotifyHandler(NotificationManager notifications, MinecraftAdapter adapter) {
        this.notifications = notifications;
        this.adapter = adapter;
    }

    public void handle(JsonObject payload) {
        String kind = ServerApiProtocol.str(payload, "kind", "announce").toLowerCase();
        String title = ServerApiProtocol.str(payload, "title", defaultTitle(kind));
        String message = ServerApiProtocol.str(payload, "message", "");
        boolean chat = ServerApiProtocol.bool(payload, "chat", kindEquals(kind, "announce") || kindEquals(kind, "event"));

        switch (kind) {
            case "friend", "friend_online", "ami" -> notifications.info(title,
                    message.isBlank() ? "A friend is online" : message);
            case "reward" -> notifications.success(title,
                    message.isBlank() ? "Reward available" : message);
            case "event" -> notifications.info(title,
                    message.isBlank() ? "Server event" : message);
            default -> notifications.info(title, message.isBlank() ? "Announcement" : message);
        }

        if (chat && !message.isBlank()) {
            adapter.displayClientMessage("Â§6[Prime] Â§f" + message);
        }
    }

    private static String defaultTitle(String kind) {
        return switch (kind) {
            case "friend", "friend_online", "ami" -> "Friends";
            case "reward" -> "Reward";
            case "event" -> "Event";
            default -> "Prime";
        };
    }

    private static boolean kindEquals(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }
}
