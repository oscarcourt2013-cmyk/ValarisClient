package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.event.ChatMessageEvent;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Parses balance from scoreboard or chat using a configurable regex. */
public final class BalanceHudModule extends Module {

    private final StringSetting balanceRegex = addSetting(new StringSetting(
            "regex", "Balance regex", "Regex with capture group 1 for the amount",
            "(?:balance|money|coins?).*?(\\d[\\d,\\.]+)"));
    private final StringSetting chatRegex = addSetting(new StringSetting(
            "chat-regex", "Chat regex", "Optional regex for balance in chat messages", ""));

    private final Element element;
    private final MinecraftAdapter adapter;
    private String lastChatBalance = "";

    public BalanceHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("balance-hud", "Balance HUD", "Shows parsed balance from scoreboard or chat", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> refreshFromScoreboard());
        listen(ChatMessageEvent.class, this::onChat);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refreshFromScoreboard();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        lastChatBalance = "";
    }

    private void refreshFromScoreboard() {
        String regex = balanceRegex.get();
        for (int i = 0; i < adapter.scoreboardLineCount(); i++) {
            String line = adapter.scoreboardLine(i);
            String match = SmpRegexUtil.firstMatch(line, regex);
            if (!match.isEmpty()) {
                element.setBalance(match);
                return;
            }
        }
        String title = adapter.scoreboardTitle();
        String titleMatch = SmpRegexUtil.firstMatch(title, regex);
        if (!titleMatch.isEmpty()) {
            element.setBalance(titleMatch);
        } else if (!lastChatBalance.isEmpty()) {
            element.setBalance(lastChatBalance);
        }
    }

    private void onChat(ChatMessageEvent event) {
        if (event.outgoing()) {
            return;
        }
        String chatRegexValue = chatRegex.get();
        if (chatRegexValue.isBlank()) {
            chatRegexValue = balanceRegex.get();
        }
        String match = SmpRegexUtil.firstMatch(event.text(), chatRegexValue);
        if (!match.isEmpty()) {
            lastChatBalance = match;
            element.setBalance(match);
        }
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private String balance = "?";
        private String text = "Balance: ?";

        Element(ThemeManager themes) {
            super("balance-hud", "Balance HUD", HudAnchor.TOP_LEFT, 4, 60);
            this.themes = themes;
        }

        void setBalance(String value) {
            if (!value.equals(balance)) {
                balance = value;
                text = "Balance: " + balance;
            }
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
