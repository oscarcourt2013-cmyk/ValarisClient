package dev.valarisclient.core.design;

import dev.valarisclient.core.adapter.RenderContext;

/** Shared Valaris logo asset and layout helpers. */
public final class ValarisLogo {

    public static final String TEXTURE = "textures/gui/logo.png";
    /** Must match the real pixel size of the texture or it renders stretched. */
    public static final int SRC_WIDTH = 512;
    public static final int SRC_HEIGHT = 518;

    private ValarisLogo() {
    }

    public static int widthForHeight(int height) {
        return Math.max(1, Math.round(height * (SRC_WIDTH / (float) SRC_HEIGHT)));
    }

    public static void draw(RenderContext ctx, int x, int y, int height, int tintArgb) {
        int width = widthForHeight(height);
        ctx.drawTexture(TEXTURE, x, y, width, height, SRC_WIDTH, SRC_HEIGHT, tintArgb);
    }

    public static void drawCentered(RenderContext ctx, int centerX, int y, int height, int tintArgb) {
        draw(ctx, centerX - widthForHeight(height) / 2, y, height, tintArgb);
    }
}
