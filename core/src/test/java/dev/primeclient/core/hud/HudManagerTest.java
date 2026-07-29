package dev.stellarclient.core.hud;

import com.google.gson.JsonObject;
import dev.stellarclient.core.adapter.RenderContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class HudManagerTest {

    private static final class BoxElement extends HudElement {
        BoxElement(String id) {
            super(id, id, HudAnchor.TOP_LEFT, 0, 0);
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return 20;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return 10;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
        }
    }

    @Test
    void renderResolvesAnchoredBounds() {
        HudManager manager = new HudManager();
        BoxElement box = manager.register(new BoxElement("box"));
        box.setLayout(HudAnchor.BOTTOM_RIGHT, -4, -4);
        box.setScale(2f);

        manager.render(new FakeRenderContext(200, 100));

        // 20x10 at scale 2 -> 40x20; bottom-right base (160, 80) plus offsets.
        assertEquals(156f, box.lastX());
        assertEquals(76f, box.lastY());
        assertEquals(40f, box.lastWidth());
        assertEquals(20f, box.lastHeight());
    }

    @Test
    void hitTestReturnsTopmostAndSkipsHidden() {
        HudManager manager = new HudManager();
        BoxElement bottom = manager.register(new BoxElement("bottom"));
        BoxElement top = manager.register(new BoxElement("top"));
        manager.render(new FakeRenderContext(200, 100));

        assertSame(top, manager.elementAt(5, 5));

        top.setVisible(false);
        assertSame(bottom, manager.elementAt(5, 5));

        assertNull(manager.elementAt(150, 90));
    }

    @Test
    void layoutRoundTripsThroughConfig() {
        HudManager manager = new HudManager();
        BoxElement box = manager.register(new BoxElement("box"));
        box.setLayout(HudAnchor.TOP_RIGHT, -8, 12);
        box.setScale(1.5f);

        JsonObject saved = manager.saveConfig().getAsJsonObject();

        HudManager fresh = new HudManager();
        fresh.loadConfig(saved); // config first...
        BoxElement restored = fresh.register(new BoxElement("box")); // ...element later

        assertEquals(HudAnchor.TOP_RIGHT, restored.anchor());
        assertEquals(-8f, restored.offsetX());
        assertEquals(12f, restored.offsetY());
        assertEquals(1.5f, restored.scale());
    }

    @Test
    void layoutSkipsHiddenElementsUnlessRequested() {
        HudManager manager = new HudManager();
        BoxElement box = manager.register(new BoxElement("box"));
        box.setVisible(false);

        manager.layout(new FakeRenderContext(200, 100));
        assertEquals(0f, box.lastWidth());

        manager.layout(new FakeRenderContext(200, 100), true);
        assertEquals(20f, box.lastWidth());

        JsonObject saved = manager.saveConfig().getAsJsonObject();
        assertFalse(saved.getAsJsonObject("box").get("visible").getAsBoolean());

        HudManager fresh = new HudManager();
        fresh.loadConfig(saved);
        BoxElement restored = fresh.register(new BoxElement("box"));
        assertFalse(restored.isVisible());
    }
}
