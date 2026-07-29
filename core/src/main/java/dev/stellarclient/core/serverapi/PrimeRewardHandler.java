package dev.stellarclient.core.serverapi;

import com.google.gson.JsonObject;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.notification.NotificationManager;

/** Handles {@code REWARD} / {@code SERVER_REWARD_AVAILABLE} packets. */
public final class PrimeRewardHandler {

    private final NotificationManager notifications;
    private final MinecraftAdapter adapter;

    public PrimeRewardHandler(NotificationManager notifications, MinecraftAdapter adapter) {
        this.notifications = notifications;
        this.adapter = adapter;
    }

    public void handle(JsonObject payload) {
        String kind = ServerApiProtocol.str(payload, "kind", ServerApiProtocol.REWARD_AVAILABLE);
        String title = ServerApiProtocol.str(payload, "title", "Prime Reward");
        String message = ServerApiProtocol.str(payload, "message", "A reward is available");
        String action = ServerApiProtocol.str(payload, "action", "message");
        String link = ServerApiProtocol.str(payload, "link", "");

        notifications.info(title, message);

        if ("link".equalsIgnoreCase(action) && !link.isBlank()) {
            adapter.openExternalLink(link);
        } else if ("open_ui".equalsIgnoreCase(action)) {
            // Stub: open social hub as a safe default surface.
            adapter.openSocialHub();
        } else if ("notification".equalsIgnoreCase(action) || "message".equalsIgnoreCase(action)) {
            if (!message.isBlank()) {
                adapter.displayClientMessage("Â§6[Prime] Â§f" + message);
            }
        }

        if (ServerApiProtocol.REWARD_AVAILABLE.equals(kind) || "REWARD".equalsIgnoreCase(kind)) {
            // already toasted
        }
    }
}
