package dev.valarisclient.core.hud.elements;

import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.design.ValarisLogo;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.theme.ThemeManager;

/** Valaris logo watermark with the Minecraft version. */
public final class WatermarkElement extends HudElement {

    private static final int PADDING = 3;
    private static final int LOGO_H = 10;

    private final ThemeManager themes;
    private final String suffix;

    public WatermarkElement(ThemeManager themes, String minecraftVersion) {
        super("watermark", "Watermark", HudAnchor.TOP_LEFT, 4, 4);
        this.themes = themes;
        this.suffix = " Client " + minecraftVersion;
        // Hidden by default — enable via Stream Branding / HUD editor. Saves a draw every frame.
        setVisible(false);
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return ValarisLogo.widthForHeight(LOGO_H) + ctx.textWidth(suffix) + PADDING * 2 + 2;
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        return Math.max(LOGO_H, ctx.fontHeight()) + PADDING * 2;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        var theme = themes.active();
        int w = measureWidth(ctx);
        int h = measureHeight(ctx);
        ctx.fillRect(0, 0, w, h, theme.background());
        ValarisLogo.draw(ctx, PADDING, PADDING + 1, LOGO_H, 0xFFFFFFFF);
        int textX = PADDING + ValarisLogo.widthForHeight(LOGO_H) + 4;
        ctx.drawText(suffix, textX, PADDING, theme.foreground(), true);
    }
}
