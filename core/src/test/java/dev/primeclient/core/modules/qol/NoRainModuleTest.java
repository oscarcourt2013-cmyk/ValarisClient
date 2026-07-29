package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.keybind.KeybindManager;
import dev.stellarclient.core.state.NoRainState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoRainModuleTest {

    private NoRainModule module;

    @BeforeEach
    void setUp() {
        ModuleManager manager = new ModuleManager(new dev.stellarclient.core.event.EventBus(), new KeybindManager());
        module = manager.register(new NoRainModule());
    }

    @AfterEach
    void reset() {
        NoRainState.setActive(false);
    }

    @Test
    void togglesNoRainState() {
        module.setEnabled(true);
        assertTrue(NoRainState.active());

        module.setEnabled(false);
        assertFalse(NoRainState.active());
    }
}
