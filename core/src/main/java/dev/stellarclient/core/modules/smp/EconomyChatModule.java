package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.event.ChatMessageEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.notification.NotificationManager;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Highlights economy chat by notifying and showing the latest matching message. */
public final class EconomyChatModule extends Module {

    private final StringSetting keywords = addSetting(new StringSetting(
            "keywords", "Keywords", "Economy keywords to watch in chat", SmpEconomyKeywords.defaults()));

    private final Element element;
    private final NotificationManager notifications;
    private String lastNotified = "";

    public EconomyChatModule(HudManager hud, ThemeManager themes, NotificationManager notifications) {
        super("economy-chat", "Economy Chat", "Alerts on economy keywords in chat", ModuleCategory.QOL);
        this.notifications = notifications;
        this.element = hud.register(new Element(themes));
        element.setVisible(false);
        listen(ChatMessageEvent.class, this::onChat);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        element.setLatest("Economy chat watch active");
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        lastNotified = "";
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing() || !SmpEconomyKeywords.matches(event.text(), keywords.get())) {
            return;
        }
        String line = event.text().length() > 50 ? event.text().substring(0, 47) + "..." : event.text();
        element.setLatest(line);
        if (!line.equals(lastNotified)) {
            lastNotified = line;
            notifications.info("Economy", line);
        }
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private String text = "Economy chat watch active";

        Element(ThemeManager themes) {
            super("economy-chat", "Economy Chat", HudAnchor.BOTTOM_LEFT, 4, -120);
            this.themes = themes;
        }

        void setLatest(String text) {
            this.text = "Eco: " + text;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return ctx.textWidth(text) + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }
    }
}
