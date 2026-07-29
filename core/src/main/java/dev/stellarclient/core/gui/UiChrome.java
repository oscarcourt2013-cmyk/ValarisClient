package dev.stellarclient.core.gui;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;

/** Shared Feather-like chrome: glass panels, cards, buttons. */
public final class UiChrome {

    private UiChrome() {
    }

    public static void glassPanel(RenderContext ctx, Theme theme, int x, int y, int w, int h) {
        int radius = PrimeDesign.RADIUS_LG;
        float fillAlpha = BlurBackdrop.isActive() ? 0.42f : 0.92f;
        float borderAlpha = BlurBackdrop.isActive() ? 0.72f : 0.55f;

        RoundedRect.softShadow(ctx, x, y, w, h, radius, 0x70000000);
        RoundedRect.border(ctx, x, y, w, h, radius, 1,
                ColorUtil.withAlpha(theme.border(), borderAlpha),
                ColorUtil.withAlpha(theme.background(), fillAlpha));
        ctx.fillGradientHorizontal(x + radius, y + 1, w - radius * 2, 1,
                ColorUtil.withAlpha(0xFFFFFFFF, BlurBackdrop.isActive() ? 0.22f : 0.12f),
                ColorUtil.withAlpha(0xFFFFFFFF, 0.02f));
        ctx.fillGradientHorizontal(x + radius, y, w - radius * 2, 2,
                ColorUtil.withAlpha(theme.accent(), BlurBackdrop.isActive() ? 0.75f : 0.95f),
                ColorUtil.withAlpha(theme.accent(), 0.05f));
        if (BlurBackdrop.isActive()) {
            ctx.fillRoundedRect(x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1),
                    ColorUtil.withAlpha(0xFFFFFFFF, 0.04f));
        }
    }

    /**
     * Flat panel for high-frequency screens (HUD editor): no soft shadow,
     * no scanline gradients â€” a few fill calls instead of thousands.
     */
    public static void editorPanel(RenderContext ctx, Theme theme, int x, int y, int w, int h) {
        int radius = PrimeDesign.RADIUS_MD;
        RoundedRect.border(ctx, x, y, w, h, radius, 1,
                ColorUtil.withAlpha(theme.border(), 0.55f),
                ColorUtil.withAlpha(theme.background(), 0.92f));
        int accentW = Math.max(0, w - radius * 2);
        if (accentW > 0) {
            ctx.fillRect(x + radius, y, accentW, 2, theme.accent());
        }
    }

    public static void cardLite(RenderContext ctx, Theme theme, int x, int y, int w, int h, boolean selected) {
        int radius = PrimeDesign.RADIUS_SM;
        float fillAlpha = BlurBackdrop.isActive()
                ? (selected ? 0.48f : 0.34f)
                : (selected ? 0.88f : 0.72f);
        int fill = ColorUtil.withAlpha(selected ? theme.surfaceElevated() : theme.background(), fillAlpha);
        RoundedRect.border(ctx, x, y, w, h, radius, 1,
                ColorUtil.withAlpha(theme.border(), selected ? 0.65f : 0.35f), fill);
    }

    public static void card(RenderContext ctx, Theme theme, int x, int y, int w, int h, boolean selected) {
        int radius = PrimeDesign.RADIUS_MD;
        float fillAlpha = BlurBackdrop.isActive()
                ? (selected ? 0.52f : 0.38f)
                : (selected ? 1f : 0.94f);
        int fill = selected ? theme.surfaceElevated() : theme.background();
        fill = ColorUtil.withAlpha(fill, fillAlpha);
        RoundedRect.softShadow(ctx, x, y, w, h, radius, selected ? 0x50000000 : 0x38000000);
        RoundedRect.border(ctx, x, y, w, h, radius, 1,
                ColorUtil.withAlpha(theme.border(), selected ? 0.75f : 0.45f), fill);
    }

    public static void button(RenderContext ctx, Theme theme, int x, int y, int w, int h,
                              boolean hover, boolean primary) {
        int radius = PrimeDesign.RADIUS_MD;
        if (primary) {
            RoundedRect.softShadow(ctx, x, y, w, h, radius, 0x55E11D2E);
            RoundedRect.fill(ctx, x, y, w, h, radius, theme.accentSecondary());
            ctx.fillGradientVertical(x + 1, y + 1, w - 2, h - 2,
                    ColorUtil.withAlpha(theme.accent(), hover ? 1f : 0.95f),
                    ColorUtil.withAlpha(theme.accentSecondary(), hover ? 1f : 0.9f));
        } else if (BlurBackdrop.isActive()) {
            RoundedRect.border(ctx, x, y, w, h, radius, 1,
                    ColorUtil.withAlpha(theme.border(), hover ? 0.85f : 0.55f),
                    ColorUtil.withAlpha(hover ? theme.backgroundLight() : theme.surfaceElevated(),
                            hover ? 0.55f : 0.42f));
        } else {
            int bg = hover ? theme.backgroundLight() : theme.surfaceElevated();
            RoundedRect.border(ctx, x, y, w, h, radius, 1,
                    ColorUtil.withAlpha(theme.border(), hover ? 0.75f : 0.4f), bg);
            if (hover) {
                ctx.fillGradientHorizontal(x + 1, y + h - 1, w - 2, 1,
                        ColorUtil.withAlpha(theme.accent(), 0.85f), ColorUtil.withAlpha(theme.accent(), 0.1f));
            }
        }
    }

    /** Rounder card chrome for the module-browser grid (bigger radius than {@link #card}). */
    public static void cardElevated(RenderContext ctx, Theme theme, int x, int y, int w, int h, boolean selected) {
        int radius = PrimeDesign.RADIUS_XL;
        float fillAlpha = BlurBackdrop.isActive()
                ? (selected ? 0.52f : 0.38f)
                : (selected ? 1f : 0.92f);
        int fill = ColorUtil.withAlpha(selected ? theme.surfaceElevated() : theme.background(), fillAlpha);
        RoundedRect.softShadow(ctx, x, y, w, h, radius, selected ? 0x50000000 : 0x30000000);
        RoundedRect.border(ctx, x, y, w, h, radius, 1,
                ColorUtil.withAlpha(theme.border(), selected ? 0.8f : 0.4f), fill);
    }

    /** Full-width stadium/pill button, e.g. the card's ENABLED / DISABLED toggle. */
    public static void pillButton(RenderContext ctx, Theme theme, int x, int y, int w, int h, boolean on) {
        int radius = h / 2;
        if (on) {
            RoundedRect.fill(ctx, x, y, w, h, radius, theme.success());
            ctx.fillGradientVertical(x + 1, y + 1, w - 2, Math.max(0, h - 2),
                    ColorUtil.withAlpha(0xFFFFFFFF, 0.10f), ColorUtil.withAlpha(0xFFFFFFFF, 0.0f));
        } else {
            RoundedRect.border(ctx, x, y, w, h, radius, 1,
                    ColorUtil.withAlpha(theme.border(), 0.55f),
                    ColorUtil.withAlpha(theme.backgroundLight(), 0.7f));
        }
    }

    public static void flatHeader(RenderContext ctx, Theme theme, int x, int y, int w, int h) {
        int radius = PrimeDesign.RADIUS_MD;
        int fill = BlurBackdrop.isActive()
                ? ColorUtil.withAlpha(theme.backgroundLight(), 0.48f)
                : theme.backgroundLight();
        RoundedRect.fill(ctx, x, y, w, h, radius, fill);
        ctx.fillGradientHorizontal(x + 1, y + h - 2, w - 2, 2,
                theme.accent(), ColorUtil.withAlpha(theme.accent(), 0.08f));
    }
}
