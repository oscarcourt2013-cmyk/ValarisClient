package dev.valarisclient.core.keybind;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.valarisclient.core.config.ConfigBinding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry and dispatcher for {@link Keybind}s.
 *
 * <p>Dispatch is O(1): a key-code index is rebuilt only when bindings change,
 * never during input handling (hot path).</p>
 */
public final class KeybindManager implements ConfigBinding {

    private final Map<String, Keybind> byId = new LinkedHashMap<>();
    private final Map<Integer, List<Keybind>> byKey = new HashMap<>();

    public Keybind register(Keybind bind) {
        Keybind previous = byId.putIfAbsent(bind.id(), bind);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate keybind id: " + bind.id());
        }
        index(bind);
        return bind;
    }

    /** Rebinds {@code bind} to {@code newKey} (or {@link Keybind#UNBOUND}). */
    public void rebind(Keybind bind, int newKey) {
        unindex(bind);
        bind.setKey(newKey);
        index(bind);
    }

    /**
     * Forwards a raw key event from the version layer.
     * Called on the client thread for every key press/release.
     */
    public void handleKey(int keyCode, boolean down) {
        List<Keybind> binds = byKey.get(keyCode);
        if (binds == null) {
            return;
        }
        for (int i = 0; i < binds.size(); i++) {
            binds.get(i).handle(down);
        }
    }

    /**
     * Polls the physical state of every bound key and fires press/release
     * transitions. Called once per client tick while no screen is open;
     * only registered keys are queried, never the whole keyboard.
     *
     * @param keyDown reads the current GLFW state of a key code
     */
    public void poll(java.util.function.IntPredicate keyDown) {
        for (Map.Entry<Integer, List<Keybind>> entry : byKey.entrySet()) {
            boolean down = keyDown.test(entry.getKey());
            List<Keybind> binds = entry.getValue();
            for (int i = 0; i < binds.size(); i++) {
                binds.get(i).handle(down);
            }
        }
    }

    /**
     * Releases every pressed keybind. Called when a screen opens so held
     * actions (zoom, sneak, ...) don't stick while the player types.
     */
    public void releaseAll() {
        for (Keybind bind : byId.values()) {
            bind.handle(false);
        }
    }

    public Keybind get(String id) {
        return byId.get(id);
    }

    public Collection<Keybind> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    private void index(Keybind bind) {
        if (bind.isBound()) {
            byKey.computeIfAbsent(bind.key(), k -> new ArrayList<>(2)).add(bind);
        }
    }

    private void unindex(Keybind bind) {
        List<Keybind> binds = byKey.get(bind.key());
        if (binds != null) {
            binds.remove(bind);
            if (binds.isEmpty()) {
                byKey.remove(bind.key());
            }
        }
    }

    // --- persistence: only keys differing from default are written ---

    @Override
    public String configKey() {
        return "keybinds";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (Keybind bind : byId.values()) {
            if (bind.key() != bind.defaultKey()) {
                json.addProperty(bind.id(), bind.key());
            }
        }
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        JsonObject json = element.getAsJsonObject();
        for (Keybind bind : byId.values()) {
            JsonElement stored = json.get(bind.id());
            int key = stored != null ? stored.getAsInt() : bind.defaultKey();
            if (key != bind.key()) {
                rebind(bind, key);
            }
        }
    }
}
