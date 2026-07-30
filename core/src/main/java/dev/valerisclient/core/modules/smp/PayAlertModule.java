package dev.valerisclient.core.modules.smp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ChatMessageEvent;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.notification.NotificationManager;
import dev.valerisclient.core.theme.ThemeManager;

/** Alerts when payment-related chat is detected. */
public final class PayAlertModule extends Module {

    private final StringSetting keywords = addSetting(new StringSetting(
            "keywords", "Keywords", "Comma-separated payment keywords",
            "paid you,received,sent you,payment,transferred,deposit,withdraw"));

    private final SmpLineHud element;
    private final NotificationManager notifications;
    private String lastAlert = "";

    public PayAlertModule(HudManager hud, ThemeManager themes, NotificationManager notifications) {
        super("pay-alert", "Pay Alert", "Alerts when someone pays or you receive money in chat", ModuleCategory.QOL);
        this.notifications = notifications;
        this.element = hud.register(new SmpLineHud(
                "pay-alert", "Pay Alert", themes, HudAnchor.BOTTOM_LEFT, 4, -96));
        element.setVisible(false);
        listen(ChatMessageEvent.class, this::onChat);
        listen(ClientTickEvent.class, event -> {
            if (!lastAlert.isEmpty()) {
                element.setText("Pay: " + lastAlert);
            }
        });
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        lastAlert = "";
        element.setText("Pay: waiting...");
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        lastAlert = "";
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing() || !SmpEconomyKeywords.matches(event.text(), keywords.get())) {
            return;
        }
        lastAlert = truncate(event.text(), 48);
        element.setText("Pay: " + lastAlert);
        notifications.info("Payment", lastAlert);
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
