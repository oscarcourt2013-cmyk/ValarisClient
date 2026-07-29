package dev.stellarclient.core.gui.clickgui;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.design.PrimeLogo;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;

/**
 * Top bar for the ClickGUI: wordmark, tab pills, search box, close button.
 *
 * <p>Degrades progressively as the effective GUI width shrinks: full labels ->
 * short labels -> drop wordmark text -> shrink search -> drop search. Render
 * and hit-testing both go through {@link #solve} so they never disagree.</p>
 */
final class ClickGuiTopBar {

    private static final int CLOSE_BTN = 16;
    private static final int TAB_H = 14;
    private static final int TAB_PAD = 12;
    private static final int SEARCH_W_MAX = 88;
    private static final int SEARCH_W_MIN = 40;
    private static final int SEARCH_H = 13;
    private static final int EDGE_PAD = 5;
    private static final int TAB_GAP = 4;

    private static final ClickGuiView[] TABS = {
            ClickGuiView.BROWSE, ClickGuiView.SETTINGS, ClickGuiView.COSMETICS, ClickGuiView.CONFIGURATIONS
    };
    private static final String[] LABELS_FULL = {"MODS", "SETTINGS", "COSMETICS", "CONFIG"};
    private static final String[] LABELS_SHORT = {"MODS", "SET", "COSM", "CFG"};

    /** Resolved top-bar geometry for one frame / one hit-test. */
    private record Solved(String[] labels, int[] tabX, int[] tabW, int tabY,
                          boolean showWordmark, int searchX, int searchW, int closeX) {
    }

    private static Solved solve(RenderContext ctx, ClickGuiBrowseLayout layout) {
        int barX = layout.topBarX();
        int barY = layout.topBarY();
        int barW = layout.topBarW();
        int barH = layout.topBarH();

        int closeX = barX + barW - EDGE_PAD - CLOSE_BTN;
        int logoW = PrimeLogo.widthForHeight(barH - 6);
        int wordmarkW = GuiLayout.labelWidth(ctx, "STELLAR CLIENT");

        // Try configurations from richest to sparsest until one fits.
        for (int attempt = 0; attempt < 5; attempt++) {
            String[] labels = attempt >= 3 ? LABELS_SHORT : LABELS_FULL;
            boolean wordmark = attempt < 1;
            int searchW = switch (attempt) {
                case 0, 1 -> SEARCH_W_MAX;
                case 2, 3 -> SEARCH_W_MIN;
                default -> 0;
            };

            int[] tabW = new int[labels.length];
            int tabsW = 0;
            for (int i = 0; i < labels.length; i++) {
                tabW[i] = GuiLayout.labelWidth(ctx, labels[i]) + TAB_PAD;
                tabsW += tabW[i] + (i > 0 ? TAB_GAP : 0);
            }

            int leftW = EDGE_PAD + logoW + (wordmark ? 4 + wordmarkW : 0);
            int rightW = (searchW > 0 ? searchW + PrimeDesign.SPACE_SM : 0) + CLOSE_BTN + EDGE_PAD;
            boolean last = attempt == 4;

            if (leftW + tabsW + rightW <= barW || last) {
                // Centre the tab strip, but never let it overlap the side clusters.
                int tabsX = barX + (barW - tabsW) / 2;
                tabsX = Math.max(barX + leftW, Math.min(tabsX, barX + barW - rightW - tabsW));
                tabsX = Math.max(barX + leftW, tabsX);

                int[] xs = new int[labels.length];
                int cursor = tabsX;
                for (int i = 0; i < labels.length; i++) {
                    xs[i] = cursor;
                    cursor += tabW[i] + TAB_GAP;
                }
                int searchX = closeX - PrimeDesign.SPACE_SM - searchW;
                return new Solved(labels, xs, tabW, barY + (barH - TAB_H) / 2,
                        wordmark, searchX, searchW, closeX);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    void render(RenderContext ctx, Theme theme, ClickGuiBrowseLayout layout, ClickGuiView activeView,
               String searchQuery, double mouseX, double mouseY) {
        int barX = layout.topBarX();
        int barY = layout.topBarY();
        int barW = layout.topBarW();
        int barH = layout.topBarH();
        if (barW <= 0 || barH <= 0) {
            return;
        }
        Solved s = solve(ctx, layout);

        UiChrome.cardElevated(ctx, theme, barX, barY, barW, barH, false);
        ctx.pushClip(barX, barY, barW, barH);

        int logoH = barH - 6;
        PrimeLogo.draw(ctx, barX + EDGE_PAD, barY + (barH - logoH) / 2, logoH, 0xFFFFFFFF);
        if (s.showWordmark()) {
            GuiLayout.label(ctx, "STELLAR CLIENT",
                    barX + EDGE_PAD + PrimeLogo.widthForHeight(logoH) + 4,
                    barY + (barH - ctx.uiFontHeight()) / 2 + 1, theme.foreground());
        }

        for (int i = 0; i < s.labels().length; i++) {
            boolean active = TABS[i] == activeView;
            boolean hover = hit(mouseX, mouseY, s.tabX()[i], s.tabY(), s.tabW()[i], TAB_H);
            GuiLayout.label(ctx, s.labels()[i],
                    s.tabX()[i] + (s.tabW()[i] - GuiLayout.labelWidth(ctx, s.labels()[i])) / 2,
                    s.tabY() + (TAB_H - ctx.uiFontHeight()) / 2 + 1,
                    active || hover ? theme.foreground() : theme.foregroundMuted());
            if (active) {
                ctx.fillRect(s.tabX()[i] + 3, s.tabY() + TAB_H - 1,
                        Math.max(0, s.tabW()[i] - 6), 1, theme.accent());
            }
        }

        if (s.searchW() > 0) {
            int searchY = barY + (barH - SEARCH_H) / 2;
            ctx.fillRoundedRect(s.searchX(), searchY, s.searchW(), SEARCH_H, SEARCH_H / 2,
                    theme.backgroundLight());
            String display = searchQuery.isBlank() ? "Search..." : searchQuery;
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, display, s.searchW() - 10),
                    s.searchX() + 5, searchY + (SEARCH_H - ctx.uiFontHeight()) / 2 + 1,
                    searchQuery.isBlank() ? theme.foregroundMuted() : theme.foreground());
        }

        int closeY = barY + (barH - CLOSE_BTN) / 2;
        boolean closeHover = hit(mouseX, mouseY, s.closeX(), closeY, CLOSE_BTN, CLOSE_BTN);
        ctx.fillRoundedRect(s.closeX(), closeY, CLOSE_BTN, CLOSE_BTN, PrimeDesign.RADIUS_SM,
                closeHover ? theme.backgroundLight() : ColorUtil.withAlpha(theme.surfaceElevated(), 0.6f));
        int xW = GuiLayout.labelWidth(ctx, "X");
        GuiLayout.label(ctx, "X", s.closeX() + (CLOSE_BTN - xW) / 2,
                closeY + (CLOSE_BTN - ctx.uiFontHeight()) / 2 + 1,
                closeHover ? theme.foreground() : theme.foregroundMuted());

        ctx.popClip();
    }

    ClickGuiView hitTab(RenderContext ctx, ClickGuiBrowseLayout layout, double mouseX, double mouseY) {
        if (layout.topBarW() <= 0) {
            return null;
        }
        Solved s = solve(ctx, layout);
        for (int i = 0; i < s.labels().length; i++) {
            if (hit(mouseX, mouseY, s.tabX()[i], s.tabY(), s.tabW()[i], TAB_H)) {
                return TABS[i];
            }
        }
        return null;
    }

    boolean hitClose(RenderContext ctx, ClickGuiBrowseLayout layout, double mouseX, double mouseY) {
        if (layout.topBarW() <= 0) {
            return false;
        }
        Solved s = solve(ctx, layout);
        int closeY = layout.topBarY() + (layout.topBarH() - CLOSE_BTN) / 2;
        return hit(mouseX, mouseY, s.closeX(), closeY, CLOSE_BTN, CLOSE_BTN);
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
