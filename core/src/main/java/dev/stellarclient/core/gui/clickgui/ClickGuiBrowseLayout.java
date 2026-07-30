package dev.stellarclient.core.gui.clickgui;

/**
 * Composed geometry for the redesigned ClickGUI browse screen: top bar,
 * left profile sidebar, module card grid, and floating bottom dock.
 *
 * <p>Single source of truth - both rendering and hit-testing must derive
 * their rectangles from here rather than recomputing offsets locally.</p>
 *
 * <p>Everything adapts to the effective GUI resolution, which is the window
 * size divided by Minecraft's GUI scale and can be as small as 320x240.
 * Card size and column count are computed, never hardcoded.</p>
 */
record ClickGuiBrowseLayout(
        int topBarX, int topBarY, int topBarW, int topBarH,
        int sidebarX, int sidebarY, int sidebarW, int sidebarH,
        int gridX, int gridY, int gridW, int gridH,
        int columns, int cardW, int cardH,
        int dockX, int dockY, int dockW, int dockH,
        int settingsPanelX, int settingsPanelY
) {
    static final int MARGIN = 6;
    static final int GAP = 5;
    static final int TOP_BAR_H = 18;
    static final int SIDEBAR_W = 82;
    static final int DOCK_H = 20;
    static final int CARD_GAP = 4;

    /** Below this effective width the sidebar is dropped to keep the grid usable. */
    static final int MIN_WIDTH_FOR_SIDEBAR = 420;

    private static final int CARD_W_MIN = 76;
    private static final int CARD_W_MAX = 124;
    private static final int CARD_H_MIN = 62;
    private static final int CARD_H_MAX = 88;
    private static final int MAX_COLUMNS = 5;

    /**
     * Geometry does not depend on whether a module's settings panel is open: that
     * panel floats <em>over</em> the grid rather than reserving space, so opening it
     * never reflows the card columns underneath.
     */
    static ClickGuiBrowseLayout compute(int screenWidth, int screenHeight) {
        int topBarW = Math.max(0, screenWidth - MARGIN * 2);
        int contentY = MARGIN + TOP_BAR_H + GAP;

        boolean showSidebar = screenWidth >= MIN_WIDTH_FOR_SIDEBAR;
        int sidebarW = showSidebar ? SIDEBAR_W : 0;
        int sidebarGap = showSidebar ? GAP : 0;

        int contentH = Math.max(0, screenHeight - contentY - MARGIN - DOCK_H - GAP);

        int gridX = MARGIN + sidebarW + sidebarGap;
        int gridW = Math.max(0, screenWidth - gridX - MARGIN);

        int columns = columnsFor(gridW);
        int cardW = cardWidthFor(gridW, columns);
        int cardH = cardHeightFor(cardW, contentH);

        int dockW = Math.min(180, Math.max(0, screenWidth - MARGIN * 2));
        int dockX = (screenWidth - dockW) / 2;
        int dockY = Math.max(0, screenHeight - MARGIN - DOCK_H);

        // Keep the floating settings Panel on screen on short windows too.
        int settingsPanelX = Math.max(MARGIN, screenWidth - Panel.WIDTH - MARGIN);
        int settingsPanelY = contentY;

        return new ClickGuiBrowseLayout(
                MARGIN, MARGIN, topBarW, TOP_BAR_H,
                MARGIN, contentY, sidebarW, contentH,
                gridX, contentY, gridW, contentH,
                columns, cardW, cardH,
                dockX, dockY, dockW, DOCK_H,
                settingsPanelX, settingsPanelY
        );
    }

    private static int columnsFor(int gridW) {
        if (gridW <= 0) {
            return 1;
        }
        // Prefer the most columns whose resulting card width stays readable.
        for (int cols = MAX_COLUMNS; cols > 1; cols--) {
            int w = (gridW - (cols - 1) * CARD_GAP) / cols;
            if (w >= CARD_W_MIN) {
                return cols;
            }
        }
        return 1;
    }

    private static int cardWidthFor(int gridW, int columns) {
        if (gridW <= 0 || columns <= 0) {
            return CARD_W_MIN;
        }
        int w = (gridW - (columns - 1) * CARD_GAP) / columns;
        return Math.max(24, Math.min(CARD_W_MAX, w));
    }

    private static int cardHeightFor(int cardW, int contentH) {
        int h = Math.round(cardW * 0.74f);
        h = Math.max(CARD_H_MIN, Math.min(CARD_H_MAX, h));
        // Never let a single row exceed the visible content area.
        int usable = contentH - ModuleCardBrowser.TAB_H - CARD_GAP;
        if (usable > 24 && h > usable) {
            h = usable;
        }
        return Math.max(24, h);
    }

    boolean showSidebar() {
        return sidebarW > 0;
    }
}
