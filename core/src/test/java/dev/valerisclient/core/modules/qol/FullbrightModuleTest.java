package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.event.EventBus;
import dev.valerisclient.core.module.ModuleManager;
import dev.valerisclient.core.keybind.KeybindManager;
import dev.valerisclient.core.state.FullbrightState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullbrightModuleTest {

    private final RecordingAdapter adapter = new RecordingAdapter();
    private EventBus bus;
    private FullbrightModule module;

    @BeforeEach
    void setUp() {
        bus = new EventBus();
        ModuleManager manager = new ModuleManager(bus, new KeybindManager());
        module = manager.register(new FullbrightModule(adapter));
        adapter.gamma = 0.5F;
    }

    @AfterEach
    void reset() {
        FullbrightState.setActive(false);
        adapter.reset();
    }

    @Test
    void raisesGammaWhileEnabledAndRestoresOnDisable() {
        module.setEnabled(true);
        bus.post(ClientTickEvent.INSTANCE);

        assertTrue(FullbrightState.active());
        assertEquals(1.0F, adapter.gamma, 0.001F);

        module.setEnabled(false);

        assertFalse(FullbrightState.active());
        assertEquals(0.5F, adapter.gamma, 0.001F);
    }

    private static final class RecordingAdapter implements MinecraftAdapter {
        private float gamma = 0.5F;

        void reset() {
            gamma = 0.5F;
        }

        @Override
        public String minecraftVersion() {
            return "test";
        }

        @Override
        public java.nio.file.Path gameDirectory() {
            return java.nio.file.Path.of(".");
        }

        @Override
        public java.nio.file.Path configDirectory() {
            return java.nio.file.Path.of(".");
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
            return false;
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

        @Override
        public float gamma() {
            return gamma;
        }

        @Override
        public void setGamma(float value) {
            this.gamma = value;
        }
    }
}
