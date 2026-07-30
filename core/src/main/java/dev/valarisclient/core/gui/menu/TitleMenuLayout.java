package dev.valarisclient.core.gui.menu;

import dev.valarisclient.core.design.ValarisLogo;

/** Compact title menu geometry. */
record TitleMenuLayout(
        int buttonX,
        int buttonY,
        int buttonW,
        int buttonH,
        int buttonGap,
        int logoX,
        int logoY,
        int logoH,
        int quitY,
        int footerY
) {
    static final int MENU_W = 112;
    static final int BUTTON_H = 15;
    static final int BUTTON_GAP = 2;

    static TitleMenuLayout compute(int screenWidth, int screenHeight, int actionCount) {
        int logoH = Math.max(28, Math.min(64, Math.round(screenHeight * 0.055f)));
        int headerBlock = logoH + 12;
        int stackH = actionCount * BUTTON_H + (actionCount - 1) * BUTTON_GAP;
        int quitGap = 8;
        int totalH = headerBlock + stackH + quitGap + 22;
        int startY = (screenHeight - totalH) / 2;
        int buttonX = (screenWidth - MENU_W) / 2;

        return new TitleMenuLayout(
                buttonX,
                startY + headerBlock,
                MENU_W,
                BUTTON_H,
                BUTTON_GAP,
                (screenWidth - ValarisLogo.widthForHeight(logoH)) / 2,
                startY,
                logoH,
                startY + headerBlock + stackH + quitGap,
                screenHeight - 12
        );
    }

    int buttonTop(int index) {
        return buttonY + index * (buttonH + buttonGap);
    }
}
