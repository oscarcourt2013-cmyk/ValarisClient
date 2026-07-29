package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.keybind.KeybindManager;
import dev.stellarclient.core.state.AlwaysDayState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlwaysDayModuleTest {

    private AlwaysDayModule module;

    @BeforeEach
    void setUp() {
        ModuleManager manager = new ModuleManager(new dev.stellarclient.core.event.EventBus(), new KeybindManager());
        module = manager.register(new AlwaysDayModule());
    }

    @AfterEach
    void reset() {
        AlwaysDayState.setActive(false);
    }

    @Test
    void togglesAlwaysDayState() {
        module.setEnabled(true);
        assertTrue(AlwaysDayState.active());

        module.setEnabled(false);
        assertFalse(AlwaysDayState.active());
    }
}
