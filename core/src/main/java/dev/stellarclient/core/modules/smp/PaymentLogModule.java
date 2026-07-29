package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.event.ChatMessageEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.ThemeManager;

import java.util.ArrayDeque;
import java.util.Deque;

/** Captures payment sent/received lines from chat. */
public final class PaymentLogModule extends Module {

    private final StringSetting keywords = addSetting(new StringSetting(
            "keywords", "Keywords", "Comma-separated payment keywords",
            "paid,sent,received,transfer,payment,balance,deposit,withdraw"));
    private final IntSetting maxLines = addSetting(new IntSetting(
            "max-lines", "Max lines", "Lines shown in the HUD", 3, 1, 6));

    private final SmpChatLogHud element;
    private final Deque<String> log = new ArrayDeque<>();

    public PaymentLogModule(HudManager hud, ThemeManager themes) {
        super("payment-log", "Payment Log", "Recent payment chat history", ModuleCategory.QOL);
        this.element = hud.register(new SmpChatLogHud(
                "payment-log", "Payment Log", themes, HudAnchor.BOTTOM_LEFT, 4, -128, 3));
        element.setVisible(false);
        listen(ChatMessageEvent.class, this::onChat);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        element.sync(log, maxLines.get());
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing() || !SmpEconomyKeywords.matches(event.text(), keywords.get())) {
            return;
        }
        log.addFirst(truncate(event.text(), 52));
        while (log.size() > maxLines.get()) {
            log.removeLast();
        }
        element.sync(log, maxLines.get());
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
