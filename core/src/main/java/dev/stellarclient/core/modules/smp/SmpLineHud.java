package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Single-line SMP HUD label shared by economy helper modules. */
public final class SmpLineHud extends HudElement {

    private static final int PADDING = 3;

    private final ThemeManager themes;
    private String text = "";

    public SmpLineHud(String id, String name, ThemeManager themes, HudAnchor anchor, float offsetX, float offsetY) {
        super(id, name, anchor, offsetX, offsetY);
        this.themes = themes;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
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
