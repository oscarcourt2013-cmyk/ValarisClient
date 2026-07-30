package dev.valerisclient.core.module;

import com.google.gson.JsonObject;
import dev.valerisclient.core.event.EventBus;
import dev.valerisclient.core.keybind.KeybindManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleManagerTest {

    private record TickEvent() {
        static final TickEvent INSTANCE = new TickEvent();
    }

    private static final class TestModule extends Module {
        final BooleanSetting flag = addSetting(new BooleanSetting("flag", "Flag", "", false));
        final IntSetting range = addSetting(new IntSetting("range", "Range", "", 5, 0, 10));
        final AtomicInteger ticks = new AtomicInteger();
        int enables;
        int disables;

        TestModule() {
            super("test", "Test", "Test module", ModuleCategory.QOL);
            listen(TickEvent.class, e -> ticks.incrementAndGet());
        }

        @Override
        protected void onEnable() {
            enables++;
        }

        @Override
        protected void onDisable() {
            disables++;
        }
    }

    private EventBus bus;
    private KeybindManager keybinds;
    private ModuleManager manager;

    @BeforeEach
    void setUp() {
        bus = new EventBus();
        keybinds = new KeybindManager();
        manager = new ModuleManager(bus, keybinds);
    }

    @Test
    void disabledModuleReceivesNoEvents() {
        TestModule module = manager.register(new TestModule());

        bus.post(TickEvent.INSTANCE);
        assertEquals(0, module.ticks.get());

        module.setEnabled(true);
        bus.post(TickEvent.INSTANCE);
        assertEquals(1, module.ticks.get());
        assertEquals(1, module.enables);

        module.setEnabled(false);
        bus.post(TickEvent.INSTANCE);
        assertEquals(1, module.ticks.get());
        assertEquals(1, module.disables);
    }

    @Test
    void toggleEventIsPosted() {
        TestModule module = manager.register(new TestModule());
        AtomicInteger toggles = new AtomicInteger();
        bus.subscribe(ModuleToggleEvent.class, e -> {
            assertEquals(module, e.module());
            toggles.incrementAndGet();
        });

        module.toggle();
        module.toggle();
        assertEquals(2, toggles.get());
    }

    @Test
    void registrationCreatesUnboundToggleKeybind() {
        TestModule module = manager.register(new TestModule());
        assertNotNull(keybinds.get("module.test"));
        assertFalse(keybinds.get("module.test").isBound());

        keybinds.rebind(keybinds.get("module.test"), 86);
        keybinds.handleKey(86, true);
        assertTrue(module.isEnabled());
    }

    @Test
    void configRoundTripsEnabledAndSettings() {
        TestModule module = manager.register(new TestModule());
        module.setEnabled(true);
        module.flag.set(true);
        module.range.set(8);

        JsonObject saved = manager.saveConfig().getAsJsonObject();

        ModuleManager fresh = new ModuleManager(bus, new KeybindManager());
        TestModule restored = fresh.register(new TestModule());
        fresh.loadConfig(saved);

        assertTrue(restored.isEnabled());
        assertTrue(restored.flag.get());
        assertEquals(8, restored.range.get());
    }

    @Test
    void configLoadedBeforeRegistrationStillApplies() {
        TestModule module = manager.register(new TestModule());
        module.setEnabled(true);
        module.range.set(3);
        JsonObject saved = manager.saveConfig().getAsJsonObject();

        ModuleManager fresh = new ModuleManager(bus, new KeybindManager());
        fresh.loadConfig(saved); // config arrives first...
        TestModule late = fresh.register(new TestModule()); // ...module registers later

        assertTrue(late.isEnabled());
        assertEquals(3, late.range.get());
    }

    @Test
    void searchMatchesNameCaseInsensitive() {
        manager.register(new TestModule());
        assertEquals(1, manager.search("TEST").size());
        assertEquals(0, manager.search("nope").size());
    }

    @Test
    void intSettingClampsOnLoad() {
        TestModule module = manager.register(new TestModule());
        module.range.set(999);
        assertEquals(10, module.range.get());
    }
}
