package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.event.ChatMessageEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Captures recent trade-related chat lines in a HUD log. */
public final class TradeLogModule extends Module {

    private final StringSetting keywords = addSetting(new StringSetting(
            "keywords", "Keywords", "Comma-separated trade keywords to capture",
            SmpEconomyKeywords.defaults()));
    private final IntSetting maxLines = addSetting(new IntSetting(
            "max-lines", "Max lines", "Lines shown in the HUD", 4, 1, 8));

    private final Element element;
    private final Deque<String> log = new ArrayDeque<>();

    public TradeLogModule(HudManager hud, ThemeManager themes) {
        super("trade-log", "Trade Log", "Shows recent economy chat messages", ModuleCategory.QOL);
        this.element = hud.register(new Element(themes, maxLines));
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
        String line = truncate(event.text(), 60);
        log.addFirst(line);
        while (log.size() > maxLines.get()) {
            log.removeLast();
        }
        element.sync(log, maxLines.get());
    }

    private static String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 3) + "...";
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;
        private static final int LINE_GAP = 2;

        private final ThemeManager themes;
        private final IntSetting maxLines;
        private List<String> lines = List.of("Trade log empty");

        Element(ThemeManager themes, IntSetting maxLines) {
            super("trade-log", "Trade Log", HudAnchor.BOTTOM_LEFT, 4, -80);
            this.themes = themes;
            this.maxLines = maxLines;
        }

        void sync(Deque<String> log, int limit) {
            if (log.isEmpty()) {
                lines = List.of("Trade log empty");
                return;
            }
            lines = new ArrayList<>(Math.min(limit, log.size()));
            int i = 0;
            for (String entry : log) {
                lines.add(entry);
                if (++i >= limit) {
                    break;
                }
            }
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            int max = ctx.textWidth("Trade log empty");
            for (String line : lines) {
                max = Math.max(max, ctx.textWidth(line));
            }
            return max + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            int lineHeight = ctx.fontHeight();
            return PADDING * 2 + lines.size() * lineHeight + (lines.size() - 1) * LINE_GAP;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            int width = measureWidth(ctx);
            int height = measureHeight(ctx);
            ctx.fillRect(0, 0, width, height, theme.background());
            int y = PADDING;
            int lineHeight = ctx.fontHeight();
            for (String line : lines) {
                ctx.drawText(line, PADDING, y, theme.foreground(), true);
                y += lineHeight + LINE_GAP;
            }
        }
    }
}
