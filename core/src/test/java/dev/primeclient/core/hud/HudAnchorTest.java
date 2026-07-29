package dev.stellarclient.core.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudAnchorTest {

    @Test
    void baseCoordinatesPerAnchor() {
        // 200x100 screen, 20x10 element
        assertEquals(0f, HudAnchor.TOP_LEFT.baseX(200, 20));
        assertEquals(0f, HudAnchor.TOP_LEFT.baseY(100, 10));

        assertEquals(90f, HudAnchor.CENTER.baseX(200, 20));
        assertEquals(45f, HudAnchor.CENTER.baseY(100, 10));

        assertEquals(180f, HudAnchor.BOTTOM_RIGHT.baseX(200, 20));
        assertEquals(90f, HudAnchor.BOTTOM_RIGHT.baseY(100, 10));
    }

    @Test
    void closestPicksScreenThirds() {
        assertEquals(HudAnchor.TOP_LEFT, HudAnchor.closest(0.1f, 0.1f));
        assertEquals(HudAnchor.CENTER, HudAnchor.closest(0.5f, 0.5f));
        assertEquals(HudAnchor.BOTTOM_RIGHT, HudAnchor.closest(0.9f, 0.9f));
        assertEquals(HudAnchor.TOP_RIGHT, HudAnchor.closest(0.9f, 0.1f));
        assertEquals(HudAnchor.MIDDLE_LEFT, HudAnchor.closest(0.1f, 0.5f));
    }
}
