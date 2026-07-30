package dev.valerisclient.core.gui.menu;

/** Geometry for the Prime pause / game menu. */
record GameMenuLayout(
        int logoY,
        int logoH,
        int brandY,
        int dividerY,
        int titleY,
        int panelX,
        int panelY,
        int panelW,
        int panelH,
        int primaryX,
        int primaryY,
        int primaryW,
        int primaryH,
        int gridX,
        int gridY,
        int cellW,
        int cellH,
        int cellGapX,
        int cellGapY,
        int quitX,
        int quitY,
        int quitW,
        int quitH,
        int footerY,
        int footerH,
        boolean brandingVisible
) {
    static final int PANEL_W = 280;
    static final int PRIMARY_H = 28;
    static final int CELL_H = 24;
    static final int QUIT_H = 26;
    static final int INNER_PAD = 14;
    static final int GAP = 8;
    static final int FOOTER_H = 28;
    /** Space after logo before PRIME wordmark. */
    static final int LOGO_TO_BRAND = 4;
    /** PRIME + CLIENT stack height (two lines). */
    static final int BRAND_TEXT_H = 24;
    /** Divider → GAME MENU → panel: keep as one tight block. */
    static final int DIVIDER_TO_TITLE = 4;
    /** Glyph room for the GAME MENU line (top of text → bottom of glyphs). */
    static final int TITLE_LINE_H = 9;
    /** Breath between GAME MENU glyphs and panel top edge. */
    static final int TITLE_TO_PANEL = 2;
    /**
     * Slight upward nudge from true vertical center so the block does not feel
     * heavy toward the sticky footer (optical center, not math center).
     */
    static final int OPTICAL_UPWARD_BIAS = 12;
    /** Minimum clear gap between panel bottom and sticky footer. */
    static final int FOOTER_CLEARANCE = 24;

    static GameMenuLayout compute(int screenWidth, int screenHeight) {
        int footerH = FOOTER_H;
        int footerY = screenHeight - footerH;
        // Usable area excludes the sticky footer — center within that band only.
        int usableH = Math.max(1, footerY);

        int panelW = Math.min(PANEL_W, Math.max(200, screenWidth - 24));

        // The composition is taller than the usable band at common GUI scales, so
        // degrade in stages rather than letting the panel run under the footer:
        // drop the branding block first, then tighten padding, then the rows.
        int innerPad = INNER_PAD;
        int gap = GAP;
        int primaryH = PRIMARY_H;
        int cellH = CELL_H;
        int quitH = QUIT_H;
        boolean branding = true;

        int logoH = Math.max(22, Math.min(34, Math.round(screenHeight * 0.042f)));
        int header = headerHeight(logoH, true);
        int panelH = panelHeight(innerPad, gap, primaryH, cellH, quitH);

        for (int stage = 0; stage < 6 && header + panelH > usableH - 4; stage++) {
            switch (stage) {
                case 0 -> branding = false;
                case 1 -> innerPad = 8;
                case 2 -> gap = 5;
                case 3 -> {
                    primaryH = 22;
                    quitH = 20;
                }
                case 4 -> cellH = 20;
                default -> {
                    innerPad = 5;
                    gap = 3;
                    primaryH = 18;
                    quitH = 17;
                    cellH = 17;
                }
            }
            header = headerHeight(logoH, branding);
            panelH = panelHeight(innerPad, gap, primaryH, cellH, quitH);
        }

        int brandY = branding ? logoH + LOGO_TO_BRAND : 0;
        int dividerY = branding ? brandY + BRAND_TEXT_H : 0;
        int titleY = branding ? dividerY + DIVIDER_TO_TITLE : 0;

        int compositionH = header + panelH;
        int free = Math.max(0, usableH - compositionH);
        // True vertical center of usable area, then a small upward optical bias.
        int startY = Math.max(4, (free / 2) - OPTICAL_UPWARD_BIAS);
        // Keep a clear gap above the footer bar when there is room to spare.
        int clearance = free > FOOTER_CLEARANCE * 2 ? FOOTER_CLEARANCE : 0;
        int maxStart = Math.max(4, usableH - compositionH - clearance);
        startY = Math.min(startY, maxStart);

        int panelX = (screenWidth - panelW) / 2;
        int panelY = startY + header;
        int contentX = panelX + innerPad;
        int contentW = panelW - innerPad * 2;
        int primaryY = panelY + innerPad;
        int gridY = primaryY + primaryH + gap;
        int cellGapX = Math.min(8, gap + 3);
        int cellW = (contentW - cellGapX) / 2;
        int quitY = gridY + cellH * 3 + gap * 2 + gap;

        return new GameMenuLayout(
                startY,
                logoH,
                startY + brandY,
                startY + dividerY,
                startY + titleY,
                panelX,
                panelY,
                panelW,
                panelH,
                contentX,
                primaryY,
                contentW,
                primaryH,
                contentX,
                gridY,
                cellW,
                cellH,
                cellGapX,
                gap,
                contentX,
                quitY,
                contentW,
                quitH,
                footerY,
                footerH,
                branding
        );
    }

    /** Height of the logo + wordmark + divider + title block above the panel. */
    private static int headerHeight(int logoH, boolean branding) {
        if (!branding) {
            return 0;
        }
        return logoH + LOGO_TO_BRAND + BRAND_TEXT_H + DIVIDER_TO_TITLE
                + TITLE_LINE_H + TITLE_TO_PANEL;
    }

    private static int panelHeight(int innerPad, int gap, int primaryH, int cellH, int quitH) {
        return innerPad * 2 + primaryH + gap + cellH * 3 + gap * 2 + gap + quitH;
    }

    int gridCellX(int col) {
        return gridX + col * (cellW + cellGapX);
    }

    int gridCellY(int row) {
        return gridY + row * (cellH + cellGapY);
    }
}
