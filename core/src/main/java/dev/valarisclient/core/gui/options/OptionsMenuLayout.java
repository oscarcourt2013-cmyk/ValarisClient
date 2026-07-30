package dev.valarisclient.core.gui.options;

/**
 * Geometry for a Valaris options screen: header, scrollable row grid, footer.
 *
 * <p>Sized from the effective GUI resolution (window / GUI scale), which can be
 * as small as 320x240 -- the row grid drops to one column and the header shrinks
 * rather than letting anything run off the edge.</p>
 */
public record OptionsMenuLayout(
        int panelX, int panelY, int panelW, int panelH,
        int headerY, int headerH,
        int gridX, int gridY, int gridW, int gridH,
        int columns, int rowW, int rowH,
        int footerX, int footerY, int footerW, int footerH,
        boolean showBranding
) {
    public static final int ROW_H = 18;
    public static final int ROW_GAP = 4;
    public static final int PAD = 10;
    public static final int FOOTER_H = 18;

    private static final int MARGIN = 6;
    private static final int HEADER_FULL = 34;
    private static final int HEADER_COMPACT = 16;
    private static final int PANEL_W_MAX = 420;
    private static final int ROW_W_MIN = 130;

    public static OptionsMenuLayout compute(int screenWidth, int screenHeight) {
        int panelW = Math.min(PANEL_W_MAX, Math.max(120, screenWidth - MARGIN * 2));
        int panelX = (screenWidth - panelW) / 2;
        int panelH = Math.max(60, screenHeight - MARGIN * 2);
        int panelY = (screenHeight - panelH) / 2;

        // Drop the logo + wordmark block when vertical room is tight.
        boolean branding = panelH >= 190;
        int headerH = branding ? HEADER_FULL : HEADER_COMPACT;

        int innerW = panelW - PAD * 2;
        int columns = innerW / 2 >= ROW_W_MIN ? 2 : 1;
        int rowW = columns == 2 ? (innerW - ROW_GAP) / 2 : innerW;

        int gridY = panelY + headerH + ROW_GAP;
        int gridH = Math.max(0, panelY + panelH - PAD - FOOTER_H - ROW_GAP - gridY);

        int footerW = Math.min(120, innerW);
        return new OptionsMenuLayout(
                panelX, panelY, panelW, panelH,
                panelY + (branding ? 6 : 4), headerH,
                panelX + PAD, gridY, innerW, gridH,
                columns, rowW, ROW_H,
                panelX + (panelW - footerW) / 2,
                panelY + panelH - PAD - FOOTER_H + 4,
                footerW, FOOTER_H,
                branding
        );
    }

    /** Top-left of row {@code index} in the scrollable grid, before scroll offset. */
    public int rowX(int index) {
        return gridX + (index % columns) * (rowW + ROW_GAP);
    }

    public int rowYOffset(int index) {
        return (index / columns) * (rowH + ROW_GAP);
    }

    public int contentHeight(int rowCount) {
        int rows = (rowCount + columns - 1) / columns;
        return Math.max(0, rows * (rowH + ROW_GAP) - ROW_GAP);
    }

    public int maxScroll(int rowCount) {
        return Math.max(0, contentHeight(rowCount) - gridH);
    }
}
