package dev.valerisclient.core.gui;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.design.PrimeDesign;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.util.ColorUtil;

/**
 * Valeris styling applied on top of unmodified vanilla screens.
 *
 * <p>Screens we have not rebuilt (world select, server list, Realms, reporting,
 * dialogs, error popups) keep every bit of their vanilla logic and widget tree;
 * only their background and widget painting is redirected here. That brands them
 * without risking the behaviour behind them -- which matters most for the screens
 * that appear when something has already gone wrong.</p>
 */
public final class VanillaSkin {

    private VanillaSkin() {
    }

    /** Backdrop behind a vanilla screen, replacing the dirt/blur menu background. */
    public static void background(RenderContext ctx, Theme theme, boolean inWorld) {
        int w = ctx.screenWidth();
        int h = ctx.screenHeight();
        if (inWorld) {
            ctx.fillRect(0, 0, w, h, theme.overlay());
        } else {
            ctx.fillGradientVertical(0, 0, w, h, theme.gradientTop(), theme.gradientBottom());
        }
        // Deeper bands top and bottom so vanilla's title and footer rows sit on
        // a darker base than the middle of the screen.
        int band = Math.max(8, h / 8);
        int edge = ColorUtil.withAlpha(0xFF000000, 0.4f);
        int clear = ColorUtil.withAlpha(0xFF000000, 0f);
        ctx.fillGradientVertical(0, 0, w, band, edge, clear);
        ctx.fillGradientVertical(0, h - band, w, band, clear, edge);
    }

    /**
     * Chrome for a vanilla button. The caller still draws the label, so the
     * button's own text handling (truncation, colour, narration) is untouched.
     */
    public static void button(RenderContext ctx, Theme theme, int x, int y, int w, int h,
                              boolean hovered, boolean active, float alpha) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int radius = Math.min(PrimeDesign.RADIUS_MD, h / 2);
        if (!active) {
            RoundedRect.border(ctx, x, y, w, h, radius, 1,
                    ColorUtil.withAlpha(theme.border(), 0.3f * alpha),
                    ColorUtil.withAlpha(theme.background(), 0.55f * alpha));
            return;
        }
        int fill = hovered ? theme.backgroundLight() : theme.surfaceElevated();
        RoundedRect.border(ctx, x, y, w, h, radius, 1,
                ColorUtil.withAlpha(theme.border(), (hovered ? 0.85f : 0.45f) * alpha),
                ColorUtil.withAlpha(fill, (hovered ? 0.95f : 0.8f) * alpha));
        if (hovered) {
            // Accent underline instead of vanilla's brighter sprite swap.
            ctx.fillGradientHorizontal(x + radius, y + h - 1, Math.max(0, w - radius * 2), 1,
                    ColorUtil.withAlpha(theme.accent(), 0.9f * alpha),
                    ColorUtil.withAlpha(theme.accent(), 0.1f * alpha));
        }
    }

    /** Label colour for a vanilla button, so disabled/hover states read consistently. */
    public static int buttonTextColor(Theme theme, boolean hovered, boolean active) {
        if (!active) {
            return theme.foregroundMuted();
        }
        return hovered ? theme.foreground() : ColorUtil.withAlpha(theme.foreground(), 0.9f);
    }
}
