package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Multi-line chat log HUD for SMP economy modules. */
public final class SmpChatLogHud extends HudElement {

    private static final int PADDING = 3;
    private static final int LINE_GAP = 2;

    private final ThemeManager themes;
    private final int defaultMaxLines;
    private List<String> lines = List.of("Log empty");

    public SmpChatLogHud(String id, String name, ThemeManager themes, HudAnchor anchor,
            float offsetX, float offsetY, int maxLines) {
        super(id, name, anchor, offsetX, offsetY);
        this.themes = themes;
        this.defaultMaxLines = maxLines;
    }

    public void sync(Deque<String> log) {
        sync(log, defaultMaxLines);
    }

    public void sync(Deque<String> log, int maxLines) {
        if (log.isEmpty()) {
            lines = List.of("Log empty");
            return;
        }
        lines = new ArrayList<>(Math.min(maxLines, log.size()));
        int i = 0;
        for (String entry : log) {
            lines.add(entry);
            if (++i >= maxLines) {
                break;
            }
        }
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        int max = ctx.textWidth("Log empty");
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
