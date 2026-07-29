package dev.stellarclient.core.gui;

import dev.stellarclient.core.adapter.RenderContext;

/** Layout helpers for Prime GUI â€” clipping, text trim, popup placement. */
public final class GuiLayout {

    private GuiLayout() {
    }

    public static String trimToWidth(RenderContext ctx, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (ctx.uiTextWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "â€¦";
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i) + ellipsis;
            if (ctx.uiTextWidth(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Clamps a popup's top-left so it stays on screen with a small margin. */
    public static int[] clampPopup(int x, int y, int width, int height, int screenW, int screenH) {
        int margin = 4;
        int maxX = Math.max(margin, screenW - width - margin);
        int maxY = Math.max(margin, screenH - height - margin);
        return new int[] {clamp(x, margin, maxX), clamp(y, margin, maxY)};
    }

    public static void label(RenderContext ctx, String text, int x, int y, int color) {
        ctx.drawUiText(text, x, y, color);
    }

    public static int labelWidth(RenderContext ctx, String text) {
        return ctx.uiTextWidth(text);
    }

    public static int tabWidth(RenderContext ctx, String label, int horizontalPadding) {
        if (ctx == null) {
            return label.length() * 6 + horizontalPadding;
        }
        return ctx.uiTextWidth(label) + horizontalPadding;
    }
}
