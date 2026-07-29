package dev.stellarclient.core.keybind;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeybindManagerTest {

    private static final int KEY_V = 86;
    private static final int KEY_B = 66;

    @Test
    void pressAndReleaseFireActionsOnce() {
        KeybindManager manager = new KeybindManager();
        AtomicInteger presses = new AtomicInteger();
        AtomicInteger releases = new AtomicInteger();
        Keybind zoom = manager.register(new Keybind("zoom", "Zoom", "QoL", KEY_V)
                .onPress(presses::incrementAndGet)
                .onRelease(releases::incrementAndGet));

        manager.handleKey(KEY_V, true);
        manager.handleKey(KEY_V, true); // key repeat must not re-fire
        assertTrue(zoom.isPressed());
        assertEquals(1, presses.get());

        manager.handleKey(KEY_V, false);
        assertFalse(zoom.isPressed());
        assertEquals(1, releases.get());
    }

    @Test
    void rebindMovesDispatch() {
        KeybindManager manager = new KeybindManager();
        AtomicInteger presses = new AtomicInteger();
        Keybind bind = manager.register(new Keybind("test", "Test", "QoL", KEY_V).onPress(presses::incrementAndGet));

        manager.rebind(bind, KEY_B);
        manager.handleKey(KEY_V, true);
        assertEquals(0, presses.get());
        manager.handleKey(KEY_B, true);
        assertEquals(1, presses.get());
    }

    @Test
    void onlyNonDefaultKeysAreSaved() {
        KeybindManager manager = new KeybindManager();
        Keybind defaulted = manager.register(new Keybind("a", "A", "QoL", KEY_V));
        Keybind rebound = manager.register(new Keybind("b", "B", "QoL", KEY_V));
        manager.rebind(rebound, KEY_B);

        JsonObject saved = manager.saveConfig().getAsJsonObject();
        assertFalse(saved.has(defaulted.id()));
        assertEquals(KEY_B, saved.get("b").getAsInt());
    }

    @Test
    void loadRestoresOverridesAndDefaults() {
        KeybindManager manager = new KeybindManager();
        Keybind bind = manager.register(new Keybind("zoom", "Zoom", "QoL", KEY_V));

        JsonObject stored = new JsonObject();
        stored.addProperty("zoom", KEY_B);
        manager.loadConfig(stored);
        assertEquals(KEY_B, bind.key());

        // A config without the entry resets to default.
        manager.loadConfig(new JsonObject());
        assertEquals(KEY_V, bind.key());
    }

    @Test
    void unboundKeybindIsNotDispatched() {
        KeybindManager manager = new KeybindManager();
        AtomicInteger presses = new AtomicInteger();
        manager.register(new Keybind("free", "Free", "QoL", Keybind.UNBOUND).onPress(presses::incrementAndGet));

        manager.handleKey(Keybind.UNBOUND, true);
        assertEquals(0, presses.get());
    }
}
