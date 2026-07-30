package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.module.ModuleManager;
import dev.valerisclient.core.keybind.KeybindManager;
import dev.valerisclient.core.state.NoRainState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoRainModuleTest {

    private NoRainModule module;

    @BeforeEach
    void setUp() {
        ModuleManager manager = new ModuleManager(new dev.valerisclient.core.event.EventBus(), new KeybindManager());
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
