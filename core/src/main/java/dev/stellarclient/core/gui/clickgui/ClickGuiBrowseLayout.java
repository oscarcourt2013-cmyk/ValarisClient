package dev.stellarclient.core.gui.clickgui;

/**
 * Composed geometry for the redesigned ClickGUI browse screen: top bar,
 * left profile sidebar, module card grid, and floating bottom dock.
 *
 * <p>Single source of truth - both rendering and hit-testing must derive
 * their rectangles from here rather than recomputing offsets locally.</p>
 */
record ClickGuiBrowseLayout(
        int topBarX, int topBarY, int topBarW, int topBarH,
        int sidebarX, int sidebarY, int sidebarW, int sidebarH,
        int gridX, int gridY, int gridW, int gridH,
        int dockX, int dockY, int dockW, int dockH,
        int settingsPanelX, int settingsPanelY
) {
    static final int MARGIN = 8;
    static final int GAP = 6;
    static final int TOP_BAR_H = 22;
    static final int SIDEBAR_W = 96;
    static final int DOCK_H = 24;

    /** Below this screen width the sidebar is hidden to keep the grid usable. */
    static final int MIN_WIDTH_FOR_SIDEBAR = 380;

    static ClickGuiBrowseLayout compute(int screenWidth, int screenHeight, boolean hasSelection) {
        int topBarX = MARGIN;
        int topBarY = MARGIN;
        int topBarW = Math.max(0, screenWidth - MARGIN * 2);

        int contentY = topBarY + TOP_BAR_H + GAP;

        boolean showSidebar = screenWidth >= MIN_WIDTH_FOR_SIDEBAR;
        int sidebarW = showSidebar ? SIDEBAR_W : 0;
        int sidebarGap = showSidebar ? GAP : 0;

        int panelReserve = hasSelection ? Panel.WIDTH + GAP : 0;
        int bottomReserve = DOCK_H + GAP;
        int contentH = Math.max(0, screenHeight - contentY - MARGIN - bottomReserve);

        int gridX = MARGIN + sidebarW + sidebarGap;
        int gridW = Math.max(0, screenWidth - gridX - MARGIN - panelReserve);

        int dockW = Math.min(220, Math.max(0, screenWidth - MARGIN * 2));
        int dockX = (screenWidth - dockW) / 2;
        int dockY = screenHeight - MARGIN - DOCK_H;

        // Matches ClickGui#refreshSelectedPanel's existing formula exactly - do not
        // diverge, the floating settings Panel is reused unchanged.
        int settingsPanelX = screenWidth - Panel.WIDTH - 12;
        int settingsPanelY = 8;

        return new ClickGuiBrowseLayout(
                topBarX, topBarY, topBarW, TOP_BAR_H,
                MARGIN, contentY, sidebarW, contentH,
                gridX, contentY, gridW, contentH,
                dockX, dockY, dockW, DOCK_H,
                settingsPanelX, settingsPanelY
        );
    }

    boolean showSidebar() {
        return sidebarW > 0;
    }
}
