package dev.valerisclient.core.gui.menu;

import dev.valerisclient.core.design.ValerisLogo;

/** Compact Feather-style ClickGUI main menu geometry. */
record ClickGuiMenuLayout(
        int menuX,
        int menuY,
        int menuW,
        int buttonH,
        int buttonGap,
        int logoX,
        int logoY,
        int logoH,
        int firstButtonY,
        int footerY
) {
    static final int MENU_W = 112;
    static final int BUTTON_H = 15;
    static final int BUTTON_GAP = 2;

    static ClickGuiMenuLayout compute(int screenWidth, int screenHeight, int buttonCount) {
        int logoH = 14;
        int versionGap = 4;
        int versionH = 9;
        int header = logoH + versionGap + versionH + 6;
        int stackH = buttonCount * BUTTON_H + (buttonCount - 1) * BUTTON_GAP;
        int totalH = header + stackH + 18;
        int startY = Math.max(16, (screenHeight - totalH) / 2);
        int menuX = (screenWidth - MENU_W) / 2;

        return new ClickGuiMenuLayout(
                menuX,
                startY,
                MENU_W,
                BUTTON_H,
                BUTTON_GAP,
                (screenWidth - ValerisLogo.widthForHeight(logoH)) / 2,
                startY,
                logoH,
                startY + header,
                startY + header + stackH + 6
        );
    }

    int buttonY(int index) {
        return firstButtonY + index * (buttonH + buttonGap);
    }
}
