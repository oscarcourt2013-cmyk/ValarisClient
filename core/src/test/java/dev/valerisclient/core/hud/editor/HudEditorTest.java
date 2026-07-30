package dev.valerisclient.core.hud.editor;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.hud.FakeRenderContext;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.theme.ThemeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudEditorTest {

    private static final int GLFW_G = 71;
    private static final int GLFW_R = 82;
    private static final int GLFW_S = 83;
    private static final int GLFW_V = 86;
    private static final int GLFW_Y = 89;
    private static final int GLFW_Z = 90;
    private static final int GLFW_RIGHT = 262;
    private static final int GLFW_LEFT = 263;
    private static final int GLFW_UP = 265;

    private static class BoxElement extends HudElement {
        private final int width;
        private final int height;

        BoxElement(String id, int width, int height) {
            super(id, "Box " + id, HudAnchor.TOP_LEFT, 0, 0);
            this.width = width;
            this.height = height;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return width;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return height;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
        }
    }

    private HudManager hud;
    private HudEditor editor;
    private BoxElement box;

    @BeforeEach
    void setUp() {
        hud = new HudManager();
        editor = new HudEditor(hud, new ThemeManager());
        box = hud.register(new BoxElement("box", 20, 10));
        hud.render(new FakeRenderContext(200, 100)); // bounds: 0,0 20x10
    }

    @Test
    void pressSelectsElementUnderCursor() {
        assertTrue(editor.mousePressed(5, 5));
        assertSame(box, editor.selected());

        assertTrue(editor.mousePressed(150, 90));
        assertSame(box, editor.selected());
        editor.mouseReleased();
    }

    @Test
    void dragMovesAndReanchorsToBottomRight() {
        editor.mousePressed(5, 5);
        // Drag so the element lands in the bottom-right third; edge guides keep it flush.
        editor.mouseDragged(185, 95, 200, 100);
        editor.mouseReleased();

        assertEquals(HudAnchor.BOTTOM_RIGHT, box.anchor());
        assertEquals(0f, box.offsetX());
        assertEquals(0f, box.offsetY());
    }

    @Test
    void dragIsClampedInsideScreen() {
        editor.mousePressed(5, 5);
        editor.mouseDragged(-50, -50, 200, 100);

        assertEquals(HudAnchor.TOP_LEFT, box.anchor());
        assertEquals(0f, box.offsetX());
        assertEquals(0f, box.offsetY());
    }

    @Test
    void dragDoesNotCrashWhenElementTallerThanScreen() {
        HudManager tallHud = new HudManager();
        HudEditor tallEditor = new HudEditor(tallHud, new ThemeManager());
        BoxElement tall = tallHud.register(new BoxElement("tall-box", 20, 300));
        tallHud.render(new FakeRenderContext(480, 270));

        tallEditor.mousePressed(5, 5);
        tallEditor.mouseDragged(132, 3, 480, 270);
        tallEditor.mouseReleased();
        tallHud.render(new FakeRenderContext(480, 270));

        assertSame(tall, tallEditor.selected());
        assertEquals(0f, tall.lastY()); // pinned to the top instead of throwing
    }

    @Test
    void dragDoesNotCrashWhenElementWiderThanScreen() {
        BoxElement wide = hud.register(new BoxElement("wide", 300, 10));
        wide.setLayout(HudAnchor.TOP_LEFT, 0, 50);
        hud.render(new FakeRenderContext(200, 100));

        assertTrue(editor.mousePressed(10, 55));
        editor.mouseDragged(60, 55, 200, 100);
        editor.mouseReleased();
        hud.render(new FakeRenderContext(200, 100));

        assertEquals(0f, wide.lastX()); // pinned to the left edge
    }

    @Test
    void dragSnapsToAnotherElementEdge() {
        BoxElement other = hud.register(new BoxElement("other", 20, 10));
        other.setLayout(HudAnchor.TOP_LEFT, 100, 20);
        hud.render(new FakeRenderContext(200, 100));

        editor.mousePressed(5, 5);
        // Raw x would be 97; "other" starts at x=100, within the 4px snap threshold.
        editor.mouseDragged(102, 60, 200, 100);
        editor.mouseReleased();
        hud.render(new FakeRenderContext(200, 100));

        assertEquals(100f, box.lastX());
    }

    @Test
    void snapCanBeToggledOffWithS() {
        assertTrue(editor.keyPressed(GLFW_S));
        editor.mousePressed(5, 5);
        editor.mouseDragged(102, 60, 200, 100);
        editor.mouseReleased();
        hud.render(new FakeRenderContext(200, 100));

        assertEquals(97f, box.lastX()); // no grid rounding, no guide snapping
    }

    @Test
    void arrowKeysNudgeSelectedElement() {
        editor.mousePressed(5, 5);
        assertTrue(editor.keyPressed(GLFW_RIGHT));
        assertTrue(editor.keyPressed(GLFW_RIGHT));
        hud.render(new FakeRenderContext(200, 100));
        assertEquals(2f, box.lastX());

        assertTrue(editor.keyPressed(GLFW_RIGHT, true, false)); // Shift = 10px
        hud.render(new FakeRenderContext(200, 100));
        assertEquals(12f, box.lastX());

        assertTrue(editor.keyPressed(GLFW_LEFT));
        assertTrue(editor.keyPressed(GLFW_UP)); // already at top: stays clamped
        hud.render(new FakeRenderContext(200, 100));
        assertEquals(11f, box.lastX());
        assertEquals(0f, box.lastY());
    }

    @Test
    void arrowKeysWithoutSelectionDoNothing() {
        assertFalse(editor.keyPressed(GLFW_RIGHT));
    }

    @Test
    void undoRestoresLayoutAndRedoReappliesIt() {
        editor.mousePressed(5, 5);
        editor.mouseDragged(50, 50, 200, 100);
        editor.mouseReleased();
        HudAnchor movedAnchor = box.anchor();
        float movedOffsetX = box.offsetX();

        assertTrue(editor.keyPressed(GLFW_Z, false, true));
        assertEquals(HudAnchor.TOP_LEFT, box.anchor());
        assertEquals(0f, box.offsetX());
        assertEquals(0f, box.offsetY());

        assertTrue(editor.keyPressed(GLFW_Y, false, true));
        assertEquals(movedAnchor, box.anchor());
        assertEquals(movedOffsetX, box.offsetX());
    }

    @Test
    void selectionClickWithoutDragLeavesNothingToUndo() {
        editor.mousePressed(5, 5);
        editor.mouseReleased();
        assertTrue(editor.keyPressed(GLFW_Z, false, true)); // handled, but nothing changes
        assertEquals(HudAnchor.TOP_LEFT, box.anchor());
        assertEquals(0f, box.offsetX());
    }

    @Test
    void scrollScalesSelectedElement() {
        editor.mousePressed(5, 5);
        assertTrue(editor.mouseScrolled(5, 5, 1, false, false));
        assertEquals(1.1f, box.scale(), 1e-5);

        assertFalse(editor.mouseScrolled(150, 90, 1, false, false));
    }

    @Test
    void shiftScrollRotatesSelectedElement() {
        editor.mousePressed(5, 5);
        assertTrue(editor.mouseScrolled(5, 5, 1, true, false));
        assertEquals(5f, box.rotation(), 1e-5);
    }

    @Test
    void ctrlScrollAdjustsOpacity() {
        editor.mousePressed(5, 5);
        assertTrue(editor.mouseScrolled(5, 5, -1, false, true));
        assertEquals(0.95f, box.opacity(), 1e-5);
    }

    @Test
    void rKeyCyclesTint() {
        editor.mousePressed(5, 5);
        assertTrue(editor.keyPressed(GLFW_R));
        assertEquals(0xFFFFFFFF, box.tintArgb());
    }

    @Test
    void vKeyTogglesVisibility() {
        editor.mousePressed(5, 5);
        assertTrue(box.isVisible());
        assertTrue(editor.keyPressed(GLFW_V));
        assertFalse(box.isVisible());
        assertTrue(editor.keyPressed(GLFW_V));
        assertTrue(box.isVisible());
    }

    /**
     * Panel geometry below assumes FakeRenderContext metrics (6px glyphs, 9px lines)
     * on a 400x300 screen; renderOverlay must run once to lay the panels out.
     */
    @Nested
    class Panels {

        private FakeRenderContext ctx;

        @BeforeEach
        void renderPanels() {
            ctx = new FakeRenderContext(400, 300);
            hud.render(ctx);
            editor.renderOverlay(ctx, 0, 0);
        }

        @Test
        void listRowClickSelectsElement() {
            assertTrue(editor.mousePressed(12, 54));
            assertSame(box, editor.selected());
        }

        @Test
        void listEyeZoneClickTogglesVisibility() {
            assertTrue(editor.mousePressed(97, 58));
            assertFalse(box.isVisible());
            assertNull(editor.selected());
        }

        @Test
        void listSelectionRecoversOffScreenElement() {
            box.setLayout(HudAnchor.TOP_LEFT, -500, -500);
            hud.render(ctx);
            editor.renderOverlay(ctx, 0, 0);

            assertTrue(editor.mousePressed(12, 54));
            hud.render(ctx);
            assertEquals(190f, box.lastX());
            assertEquals(145f, box.lastY());
        }

        @Test
        void scrollOverListPanelIsConsumedWithoutZooming() {
            for (int i = 0; i < 20; i++) {
                hud.register(new BoxElement("extra-" + i, 20, 10));
            }
            hud.render(ctx);
            editor.renderOverlay(ctx, 0, 0);

            assertTrue(editor.mouseScrolled(50, 80, 1, false, false));
            assertEquals(1f, box.scale(), 1e-5);
        }

        @Test
        void toolbarGridButtonTogglesLikeGKey() {
            assertTrue(editor.mousePressed(121, 11)); // "Grid" button
            assertTrue(editor.keyPressed(GLFW_G));    // both paths flip the same flag
        }

        @Test
        void toolbarResetAllRestoresDefaults() {
            box.setLayout(HudAnchor.BOTTOM_RIGHT, -30, -30);
            box.setScale(2f);
            box.setVisible(false);

            // "Reset All" sits at x=306..372, y=10..24 on a 400x300 canvas. The
            // toolbar gained a "Hidden" toggle before it, so it moved right.
            assertTrue(editor.mousePressed(339, 17));
            assertEquals(HudAnchor.TOP_LEFT, box.anchor());
            assertEquals(0f, box.offsetX());
            assertEquals(1f, box.scale(), 1e-5);
            assertTrue(box.isVisible());
        }

        @Test
        void propertiesDockDoesNotBlockCanvasDrag() {
            editor.mousePressed(12, 54);
            editor.renderOverlay(ctx, 0, 0);

            assertTrue(editor.mousePressed(200, 150));
            editor.mouseDragged(260, 180, 400, 300);
            editor.mouseReleased();
            hud.render(ctx);

            assertTrue(box.lastX() > 0f || box.lastY() > 0f);
        }

        @Test
        void propertiesVisibilityButtonToggles() {
            editor.mousePressed(12, 54);
            editor.renderOverlay(ctx, 0, 0);

            // On a 400x300 canvas the dock stacks its sliders, so it is 106px tall
            // and starts at y=165 (300 - 25 hint reserve - 106 - 4). The visibility
            // button sits on its first row at x=192..232, y=170..184.
            assertTrue(editor.mousePressed(208, 177));
            assertFalse(box.isVisible());
        }

        @Test
        void dragAfterListSelectMovesElement() {
            editor.mousePressed(12, 54);
            assertSame(box, editor.selected());
            editor.mousePressed(160, 120);
            editor.mouseDragged(220, 160, 400, 300);
            editor.mouseReleased();
            hud.render(ctx);

            assertTrue(box.lastX() > 0f || box.lastY() > 0f);
        }

        @Test
        void canvasPressArmsDragWithoutMovingUntilMotion() {
            editor.mousePressed(12, 54);
            editor.mousePressed(160, 120);
            hud.render(ctx);
            assertEquals(0f, box.lastX());
            assertEquals(0f, box.lastY());
        }

        @Test
        void clickThroughListPaddingSelectsElementUnderneath() {
            box.setLayout(HudAnchor.TOP_LEFT, 5, 120);
            hud.render(ctx);
            editor.renderOverlay(ctx, 0, 0);

            assertTrue(editor.mousePressed(20, 125));
            assertSame(box, editor.selected());
        }

        @Test
        void clickOnPanelDoesNotDeselectOrGrabElements() {
            editor.mousePressed(12, 54);
            editor.renderOverlay(ctx, 0, 0);

            // Inside the list panel (y 34..172 here) but below the only row, which
            // ends at y=65 -- the empty area must pass through.
            assertFalse(editor.mousePressed(60, 100));
            assertSame(box, editor.selected());
            editor.mouseDragged(200, 200, 400, 300);
            hud.render(ctx);
            assertEquals(0f, box.lastX()); // drag was not armed from empty panel click
        }
    }
}
