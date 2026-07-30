package dev.valarisclient.v1_21_11.render;

import dev.valarisclient.core.hook.ValarisHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Tab-list client badge. Uses a solid plate + glyph (not the full logo texture),
 * because scaling the 512x279 transparent logo to 8px is effectively invisible.
 */
public final class ValarisTabBadgeRenderer {

    private static final String GLYPH = "P";
    private static final int PAD_X = 3;
    private static final int ACCENT_W = 2;

    private ValarisTabBadgeRenderer() {
    }

    public static int height() {
        return 8;
    }

    public static int width() {
        Font font = Minecraft.getInstance().font;
        return ACCENT_W + PAD_X + font.width(GLYPH) + PAD_X;
    }

    public static void draw(GuiGraphics graphics, int x, int y) {
        int h = height();
        int w = width();
        int bg = ValarisHooks.clientBadgeBackground();
        int accent = ValarisHooks.clientBadgeAccent();
        int fg = ValarisHooks.clientBadgeForeground();
        graphics.fill(x, y, x + w, y + h, bg);
        graphics.fill(x, y, x + ACCENT_W, y + h, accent);
        graphics.drawString(Minecraft.getInstance().font, GLYPH, x + ACCENT_W + PAD_X, y, fg, false);
    }
}
