package dev.valerisclient.core.gui;

import dev.valerisclient.core.adapter.RenderContext;

/** Software rounded-rectangle fills (Feather-style panels without a shader). */
public final class RoundedRect {

    private RoundedRect() {
    }

    public static void fill(RenderContext ctx, int x, int y, int width, int height, int radius, int argb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (radius <= 0) {
            ctx.fillRect(x, y, width, height, argb);
            return;
        }
        radius = Math.min(radius, Math.min(width, height) / 2);
        ctx.fillRect(x + radius, y, width - radius * 2, height, argb);
        ctx.fillRect(x, y + radius, width, height - radius * 2, argb);
        fillCorner(ctx, x, y, radius, argb, true, true);
        fillCorner(ctx, x + width - radius, y, radius, argb, false, true);
        fillCorner(ctx, x, y + height - radius, radius, argb, true, false);
        fillCorner(ctx, x + width - radius, y + height - radius, radius, argb, false, false);
    }

    /** Outer rounded rect minus inner — cheap 1px border. */
    public static void border(RenderContext ctx, int x, int y, int width, int height,
                              int radius, int thickness, int borderArgb, int innerArgb) {
        fill(ctx, x, y, width, height, radius, borderArgb);
        fill(ctx, x + thickness, y + thickness,
                width - thickness * 2, height - thickness * 2,
                Math.max(0, radius - thickness), innerArgb);
    }

    /**
     * Cheap drop shadow: two flat offset rects.
     * Avoids multi-layer rounded fills (thousands of draw calls on tall panels).
     *
     * @param radius unused — kept for call-site compatibility with rounded panel API
     */
    public static void softShadow(RenderContext ctx, int x, int y, int width, int height,
                                  int radius, int shadowArgb) {
        int baseAlpha = (shadowArgb >>> 24) & 0xFF;
        if (baseAlpha <= 0 || width <= 0 || height <= 0) {
            return;
        }
        int rgb = shadowArgb & 0x00FFFFFF;
        int aOuter = Math.round(baseAlpha * 0.14f);
        int aInner = Math.round(baseAlpha * 0.28f);
        if (aOuter > 0) {
            ctx.fillRect(x - 3, y + 2, width + 6, height + 4, (aOuter << 24) | rgb);
        }
        if (aInner > 0) {
            ctx.fillRect(x - 1, y + 1, width + 2, height + 2, (aInner << 24) | rgb);
        }
    }

    /** One horizontal span per row — not 1×1 pixels (radius² draw calls). */
    private static void fillCorner(RenderContext ctx, int left, int top, int radius, int argb,
                                   boolean leftArc, boolean topArc) {
        float r2 = (float) radius * radius;
        for (int dy = 0; dy < radius; dy++) {
            float py = topArc ? (radius - dy - 0.5f) : (dy + 0.5f);
            float py2 = py * py;
            if (py2 > r2) {
                continue;
            }
            float maxPx = (float) Math.sqrt(r2 - py2);
            if (leftArc) {
                int dx0 = Math.max(0, (int) Math.ceil(radius - 0.5f - maxPx));
                if (dx0 < radius) {
                    ctx.fillRect(left + dx0, top + dy, radius - dx0, 1, argb);
                }
            } else {
                int dx1 = Math.min(radius - 1, (int) Math.floor(maxPx - 0.5f));
                if (dx1 >= 0) {
                    ctx.fillRect(left, top + dy, dx1 + 1, 1, argb);
                }
            }
        }
    }
}
