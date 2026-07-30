package dev.valarisclient.core.gui.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TitleMenuRendererTest {

    private static final int W = 854;
    private static final int H = 480;

    private final TitleMenuRenderer renderer = new TitleMenuRenderer();

    @Test
    void hitActionDetectsSingleplayerButton() {
        TitleMenuLayout layout = TitleMenuLayout.compute(W, H, 4);
        double mx = layout.buttonX() + layout.buttonW() / 2.0;
        double my = layout.buttonTop(0) + layout.buttonH() / 2.0;

        assertEquals(TitleMenuAction.SINGLEPLAYER, renderer.hitAction(mx, my, W, H));
    }

    @Test
    void hitActionReturnsNullOutsideButtons() {
        assertNull(renderer.hitAction(10, 10, W, H));
    }
}
