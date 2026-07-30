package dev.valarisclient.core.modules.qol;

import dev.valarisclient.core.module.ModuleManager;
import dev.valarisclient.core.keybind.KeybindManager;
import dev.valarisclient.core.state.LowFireState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LowFireModuleTest {

    private LowFireModule module;

    @BeforeEach
    void setUp() {
        ModuleManager manager = new ModuleManager(new dev.valarisclient.core.event.EventBus(), new KeybindManager());
        module = manager.register(new LowFireModule());
    }

    @AfterEach
    void reset() {
        LowFireState.setActive(false);
        LowFireState.setHeightOffset(0.0F);
    }

    @Test
    void togglesLowFireState() {
        module.setEnabled(true);
        assertTrue(LowFireState.active());

        module.setEnabled(false);
        assertFalse(LowFireState.active());
    }
}
