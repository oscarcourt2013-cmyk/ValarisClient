package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.hud.FakeRenderContext;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.theme.ThemeManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the CPS module end to end through the HUD pipeline with a fake
 * adapter: edge detection, the 1-second window and visibility binding.
 */
class CpsRingBufferTest {

    private static final class FakeAdapter implements MinecraftAdapter {
        boolean leftDown;

        @Override
        public String minecraftVersion() {
            return "test";
        }

        @Override
        public Path gameDirectory() {
            return Path.of(".");
        }

        @Override
        public Path configDirectory() {
            return Path.of(".");
        }

        @Override
        public boolean isInGame() {
            return true;
        }

        @Override
        public boolean isScreenOpen() {
            return false;
        }

        @Override
        public boolean isKeyDown(int glfwKey) {
            return false;
        }

        @Override
        public boolean isMouseButtonDown(int glfwButton) {
            return glfwButton == 0 && leftDown;
        }

        @Override
        public int fps() {
            return 60;
        }

        @Override
        public boolean hasPlayer() {
            return false;
        }

        @Override
        public double playerX() {
            return 0;
        }

        @Override
        public double playerY() {
            return 0;
        }

        @Override
        public double playerZ() {
            return 0;
        }

        @Override
        public void runOnClientThread(Runnable task) {
            task.run();
        }

        @Override
        public void openHudEditor() {
        }

        @Override
        public void openClickGui() {
        }
    }

    @Test
    void moduleTogglesElementVisibilityAndRenders() {
        HudManager hud = new HudManager();
        FakeAdapter adapter = new FakeAdapter();
        CpsCounterModule module = new CpsCounterModule(hud, new ThemeManager(), adapter);

        assertFalse(hud.get("cps").isVisible());
        module.setEnabled(true);
        assertTrue(hud.get("cps").isVisible());

        // Simulate a few frames with click edges; must not throw and must draw.
        FakeRenderContext ctx = new FakeRenderContext(400, 240);
        for (int frame = 0; frame < 10; frame++) {
            adapter.leftDown = frame % 2 == 0; // 5 press edges
            hud.render(ctx);
        }
        assertTrue(ctx.fillCalls > 0);
        assertTrue(ctx.textCalls > 0);

        module.setEnabled(false);
        assertFalse(hud.get("cps").isVisible());
    }
}
