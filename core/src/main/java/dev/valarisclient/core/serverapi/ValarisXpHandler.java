package dev.valarisclient.core.serverapi;

import com.google.gson.JsonObject;
import dev.valarisclient.core.notification.NotificationManager;

/** Handles {@code XP} packets from partner servers. */
public final class ValarisXpHandler {

    private final ValarisAccountManager account;
    private final NotificationManager notifications;

    public ValarisXpHandler(ValarisAccountManager account, NotificationManager notifications) {
        this.account = account;
        this.notifications = notifications;
    }

    public void handle(JsonObject payload) {
        int amount = ServerApiProtocol.integer(payload, "amount", 0);
        if (amount <= 0) {
            amount = ServerApiProtocol.integer(payload, "xp", 0);
        }
        if (amount <= 0) {
            return;
        }
        String reason = ServerApiProtocol.str(payload, "reason",
                ServerApiProtocol.str(payload, "kind", ServerApiProtocol.XP_EVENT));
        account.addXp(amount);
        String label = switch (reason) {
            case ServerApiProtocol.XP_PLAYTIME -> "Playtime";
            case ServerApiProtocol.XP_ACHIEVEMENT -> "Achievement";
            default -> "Event";
        };
        notifications.success("Valaris XP", "⚡ +" + amount + " XP Valaris (" + label + ")");
    }
}
