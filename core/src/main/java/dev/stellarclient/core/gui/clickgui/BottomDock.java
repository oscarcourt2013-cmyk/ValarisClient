package dev.stellarclient.core.gui.clickgui;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;

/** Floating icon-only quick-action dock at the bottom of the ClickGUI browse screen. */
final class BottomDock {

    enum Action {
        HUD_EDITOR("H"),
        COSMETICS("*"),
        CONFIGURATIONS("#"),
        SETTINGS("S");

        final String glyph;

        Action(String glyph) {
            this.glyph = glyph;
        }
    }

    private static final int BTN = 16;
    private static final int GAP = 3;

    void render(RenderContext ctx, Theme theme, ClickGuiBrowseLayout layout, double mouseX, double mouseY) {
        int w = layout.dockW();
        int h = layout.dockH();
        if (w <= 0 || h <= 0) {
            return;
        }
        int x = layout.dockX();
        int y = layout.dockY();

        UiChrome.cardElevated(ctx, theme, x, y, w, h, false);

        Action[] actions = Action.values();
        int totalBtnW = actions.length * BTN + (actions.length - 1) * GAP;
        int startX = x + (w - totalBtnW) / 2;
        int btnY = y + (h - BTN) / 2;
        for (int i = 0; i < actions.length; i++) {
            int bx = startX + i * (BTN + GAP);
            boolean hover = hit(mouseX, mouseY, bx, btnY, BTN, BTN);
            int fill = hover
                    ? ColorUtil.withAlpha(theme.backgroundLight(), 0.7f)
                    : ColorUtil.withAlpha(theme.surfaceElevated(), 0.5f);
            ctx.fillRoundedRect(bx, btnY, BTN, BTN, PrimeDesign.RADIUS_SM, fill);
            String glyph = actions[i].glyph;
            int glyphW = GuiLayout.labelWidth(ctx, glyph);
            GuiLayout.label(ctx, glyph, bx + (BTN - glyphW) / 2, btnY + (BTN - ctx.uiFontHeight()) / 2 + 1,
                    hover ? theme.accent() : theme.foregroundMuted());
        }
    }

    Action hit(ClickGuiBrowseLayout layout, double mouseX, double mouseY) {
        int w = layout.dockW();
        int h = layout.dockH();
        if (w <= 0 || h <= 0) {
            return null;
        }
        int x = layout.dockX();
        int y = layout.dockY();
        Action[] actions = Action.values();
        int totalBtnW = actions.length * BTN + (actions.length - 1) * GAP;
        int startX = x + (w - totalBtnW) / 2;
        int btnY = y + (h - BTN) / 2;
        for (int i = 0; i < actions.length; i++) {
            int bx = startX + i * (BTN + GAP);
            if (hit(mouseX, mouseY, bx, btnY, BTN, BTN)) {
                return actions[i];
            }
        }
        return null;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
