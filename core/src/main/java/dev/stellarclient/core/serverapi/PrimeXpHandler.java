package dev.stellarclient.core.serverapi;

import com.google.gson.JsonObject;
import dev.stellarclient.core.notification.NotificationManager;

/** Handles {@code XP} packets from partner servers. */
public final class PrimeXpHandler {

    private final PrimeAccountManager account;
    private final NotificationManager notifications;

    public PrimeXpHandler(PrimeAccountManager account, NotificationManager notifications) {
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
        notifications.success("Prime XP", "âš¡ +" + amount + " XP Prime (" + label + ")");
    }
}
