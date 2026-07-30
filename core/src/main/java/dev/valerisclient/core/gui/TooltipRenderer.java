package dev.valerisclient.core.gui;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.theme.Theme;

/** Lightweight tooltip overlay. */
public final class TooltipRenderer {

    private String text = "";
    private int x;
    private int y;
    private int showMillis;

    public void show(String text, int x, int y) {
        this.text = text == null ? "" : text;
        this.x = x;
        this.y = y;
        this.showMillis = 3000;
    }

    public void tick(int deltaMillis) {
        if (showMillis > 0) {
            showMillis -= deltaMillis;
        }
    }

    public void render(RenderContext ctx, Theme theme) {
        if (showMillis <= 0 || text.isEmpty()) {
            return;
        }
        int w = ctx.textWidth(text) + 8;
        int h = ctx.fontHeight() + 6;
        ctx.fillRect(x, y, w, h, theme.surfaceElevated());
        ctx.drawText(text, x + 4, y + 3, theme.foreground(), true);
    }
}
