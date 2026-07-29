package dev.stellarclient.core.crosshair;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.util.ColorUtil;

/** Renders crosshair styles into a HUD-local coordinate space. */
public final class CrosshairRenderer {

    private CrosshairRenderer() {
    }

    public static void render(RenderContext ctx, CrosshairConfig config) {
        int color = config.effectiveColor();
        int size = Math.max(5, config.size);
        int center = size / 2;
        int arm = Math.max(1, config.armLength);
        int thick = Math.max(1, config.thickness);
        int gap = Math.max(0, config.gap);

        if (config.rotation != 0f) {
            ctx.pushTransform(0, 0, 1f, config.rotation, center, center);
        }

        switch (config.style) {
            case DOT -> drawDot(ctx, center, thick + 1, color, config.outline);
            case CIRCLE -> drawCircleApprox(ctx, center, arm + gap, thick, color, config.outline);
            case T_SHAPE -> {
                drawBar(ctx, center - arm, center, arm * 2 + 1, thick, color, config.outline);
                drawBar(ctx, center, center + gap, thick, arm, color, config.outline);
            }
            case DYNAMIC -> {
                int dynArm = arm + (int) (Math.sin(System.currentTimeMillis() / 200.0) * 2);
                drawClassic(ctx, center, dynArm, thick, gap, color, config.outline);
            }
            case CUSTOM, CLASSIC -> drawClassic(ctx, center, arm, thick, gap, color, config.outline);
        }

        if (config.rotation != 0f) {
            ctx.popTransform();
        }
    }

    public static void renderPreview(RenderContext ctx, CrosshairConfig config, int boxSize) {
        int cx = boxSize / 2;
        int cy = boxSize / 2;
        ctx.pushTransform(cx - config.size / 2f, cy - config.size / 2f, 1f);
        render(ctx, config);
        ctx.popTransform();
    }

    private static void drawClassic(RenderContext ctx, int center, int arm, int thick, int gap,
                                    int color, boolean outline) {
        drawBar(ctx, center - arm, center, arm - gap, thick, color, outline);
        drawBar(ctx, center + gap + 1, center, arm - gap, thick, color, outline);
        drawBar(ctx, center, center - arm, thick, arm - gap, color, outline);
        drawBar(ctx, center, center + gap + 1, thick, arm - gap, color, outline);
    }

    private static void drawDot(RenderContext ctx, int center, int size, int color, boolean outline) {
        int x = center - size / 2;
        int y = center - size / 2;
        if (outline) {
            ctx.fillRect(x - 1, y - 1, size + 2, size + 2, 0xFF000000);
        }
        ctx.fillRect(x, y, size, size, color);
    }

    private static void drawBar(RenderContext ctx, int x, int y, int w, int h, int color, boolean outline) {
        if (outline) {
            ctx.fillRect(x - 1, y - 1, w + 2, h + 2, 0xFF000000);
        }
        ctx.fillRect(x, y, w, h, color);
    }

    private static void drawCircleApprox(RenderContext ctx, int center, int radius, int thick, int color, boolean outline) {
        for (int deg = 0; deg < 360; deg += 6) {
            double rad = Math.toRadians(deg);
            int x = center + (int) Math.round(Math.cos(rad) * radius) - thick / 2;
            int y = center + (int) Math.round(Math.sin(rad) * radius) - thick / 2;
            if (outline) {
                ctx.fillRect(x - 1, y - 1, thick + 2, thick + 2, 0xFF000000);
            }
            ctx.fillRect(x, y, thick, thick, color);
        }
    }
}
