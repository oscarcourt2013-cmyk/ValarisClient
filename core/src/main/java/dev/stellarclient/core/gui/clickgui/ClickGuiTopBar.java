package dev.stellarclient.core.gui.clickgui;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.design.PrimeLogo;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;

/** Top bar for the redesigned ClickGUI: wordmark, tab pills, search box, close button. */
final class ClickGuiTopBar {

    static final int CLOSE_BTN = 20;
    private static final int TAB_H = 18;
    private static final int SEARCH_W = 96;
    private static final int SEARCH_H = 16;

    private static final ClickGuiView[] TABS = {
            ClickGuiView.BROWSE, ClickGuiView.SETTINGS, ClickGuiView.COSMETICS, ClickGuiView.CONFIGURATIONS
    };
    private static final String[] TAB_LABELS = {"MODS", "SETTINGS", "COSMETICS", "CONFIG"};

    void render(RenderContext ctx, Theme theme, ClickGuiBrowseLayout layout, ClickGuiView activeView,
               String searchQuery, double mouseX, double mouseY) {
        int x = layout.topBarX();
        int y = layout.topBarY();
        int w = layout.topBarW();
        int h = layout.topBarH();

        UiChrome.cardElevated(ctx, theme, x, y, w, h, false);

        int logoH = h - 8;
        PrimeLogo.draw(ctx, x + 8, y + (h - logoH) / 2, logoH, 0xFFFFFFFF);
        int wordmarkX = x + 8 + PrimeLogo.widthForHeight(logoH) + 6;
        GuiLayout.label(ctx, "STELLAR CLIENT", wordmarkX, y + (h - ctx.uiFontHeight()) / 2 + 1, theme.foreground());

        renderTabs(ctx, theme, layout, activeView, mouseX, mouseY);

        int searchX = closeButtonX(x, w) - PrimeDesign.SPACE_SM - SEARCH_W;
        int searchY = y + (h - SEARCH_H) / 2;
        ctx.fillRoundedRect(searchX, searchY, SEARCH_W, SEARCH_H, SEARCH_H / 2, theme.backgroundLight());
        String display = searchQuery.isBlank() ? "Search..." : searchQuery;
        int color = searchQuery.isBlank() ? theme.foregroundMuted() : theme.foreground();
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, display, SEARCH_W - 16),
                searchX + 8, searchY + (SEARCH_H - ctx.uiFontHeight()) / 2 + 1, color);

        int closeX = closeButtonX(x, w);
        int closeY = y + (h - CLOSE_BTN) / 2;
        boolean closeHover = hit(mouseX, mouseY, closeX, closeY, CLOSE_BTN, CLOSE_BTN);
        int closeFill = closeHover ? theme.backgroundLight() : ColorUtil.withAlpha(theme.surfaceElevated(), 0.6f);
        ctx.fillRoundedRect(closeX, closeY, CLOSE_BTN, CLOSE_BTN, PrimeDesign.RADIUS_SM, closeFill);
        String closeGlyph = "X";
        int closeGlyphW = GuiLayout.labelWidth(ctx, closeGlyph);
        GuiLayout.label(ctx, closeGlyph, closeX + (CLOSE_BTN - closeGlyphW) / 2,
                closeY + (CLOSE_BTN - ctx.uiFontHeight()) / 2 + 1,
                closeHover ? theme.foreground() : theme.foregroundMuted());
    }

    private void renderTabs(RenderContext ctx, Theme theme, ClickGuiBrowseLayout layout, ClickGuiView activeView,
                            double mouseX, double mouseY) {
        int[] tabX = new int[1];
        int tabY = layout.topBarY() + (layout.topBarH() - TAB_H) / 2;
        forEachTab(ctx, layout, (i, tx, tw) -> {
            boolean active = TABS[i] == activeView;
            boolean hover = hit(mouseX, mouseY, tx, tabY, tw, TAB_H);
            int textColor = active || hover ? theme.foreground() : theme.foregroundMuted();
            int labelW = GuiLayout.labelWidth(ctx, TAB_LABELS[i]);
            GuiLayout.label(ctx, TAB_LABELS[i], tx + (tw - labelW) / 2,
                    tabY + (TAB_H - ctx.uiFontHeight()) / 2 + 1, textColor);
            if (active) {
                ctx.fillRect(tx + 4, tabY + TAB_H - 2, tw - 8, 1, theme.accent());
            }
        });
    }

    ClickGuiView hitTab(RenderContext ctx, ClickGuiBrowseLayout layout, double mouseX, double mouseY) {
        int tabY = layout.topBarY() + (layout.topBarH() - TAB_H) / 2;
        ClickGuiView[] result = new ClickGuiView[1];
        forEachTab(ctx, layout, (i, tx, tw) -> {
            if (result[0] == null && hit(mouseX, mouseY, tx, tabY, tw, TAB_H)) {
                result[0] = TABS[i];
            }
        });
        return result[0];
    }

    private interface TabVisitor {
        void visit(int index, int tabX, int tabWidth);
    }

    private static void forEachTab(RenderContext ctx, ClickGuiBrowseLayout layout, TabVisitor visitor) {
        int[] widths = new int[TAB_LABELS.length];
        int totalW = 0;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            widths[i] = GuiLayout.labelWidth(ctx, TAB_LABELS[i]) + 20;
            totalW += widths[i] + PrimeDesign.SPACE_MD;
        }
        totalW -= PrimeDesign.SPACE_MD;
        int tabX = layout.topBarX() + (layout.topBarW() - totalW) / 2;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            visitor.visit(i, tabX, widths[i]);
            tabX += widths[i] + PrimeDesign.SPACE_MD;
        }
    }

    boolean hitClose(ClickGuiBrowseLayout layout, double mouseX, double mouseY) {
        int closeX = closeButtonX(layout.topBarX(), layout.topBarW());
        int closeY = layout.topBarY() + (layout.topBarH() - CLOSE_BTN) / 2;
        return hit(mouseX, mouseY, closeX, closeY, CLOSE_BTN, CLOSE_BTN);
    }

    private static int closeButtonX(int x, int w) {
        return x + w - PrimeDesign.SPACE_MD - CLOSE_BTN;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
