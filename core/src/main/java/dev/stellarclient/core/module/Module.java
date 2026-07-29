package dev.stellarclient.core.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.event.EventBus;
import dev.stellarclient.core.i18n.PrimeLang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class of every StellarClient module.
 *
 * <p>Modules declare their event interests once, in the constructor, via
 * {@link #listen}. Listeners are attached to the {@link EventBus} only while
 * the module is enabled â€” a disabled module costs strictly nothing: no
 * listener on the bus, no tick, no render.</p>
 *
 * <p>Subclasses override {@link #onEnable()}/{@link #onDisable()} for state
 * setup/teardown and declare {@link Setting}s via {@link #addSetting}.</p>
 */
public abstract class Module {

    private record ListenerSpec<T>(Class<T> eventType, int priority, Consumer<T> handler) {
    }

    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<Setting> settings = new ArrayList<>(4);
    private final List<ListenerSpec<?>> listenerSpecs = new ArrayList<>(2);
    private final List<EventBus.Subscription> activeSubscriptions = new ArrayList<>(2);

    private EventBus eventBus;
    private Runnable toggleCallback;
    private boolean enabled;

    /**
     * @param id stable identifier used in configs and keybinds, e.g. {@code "keystrokes"}
     */
    protected Module(String id, String name, String description, ModuleCategory category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
    }

    // --- declaration API for subclasses ---

    /** Declares an event interest, active only while the module is enabled. */
    protected final <T> void listen(Class<T> eventType, Consumer<T> handler) {
        listen(eventType, 0, handler);
    }

    /** Same as {@link #listen(Class, Consumer)} with an explicit priority. */
    protected final <T> void listen(Class<T> eventType, int priority, Consumer<T> handler) {
        listenerSpecs.add(new ListenerSpec<>(eventType, priority, handler));
    }

    /** Registers a setting; returns it so it can be kept in a typed field. */
    protected final <S extends Setting> S addSetting(S setting) {
        for (Setting existing : settings) {
            if (existing.id().equals(setting.id())) {
                throw new IllegalArgumentException(id + ": duplicate setting id '" + setting.id() + "'");
            }
        }
        setting.bindModule(id);
        settings.add(setting);
        return setting;
    }

    /** State setup when the module turns on. Optional. */
    protected void onEnable() {
    }

    /** State teardown when the module turns off. Optional. */
    protected void onDisable() {
    }

    // --- public API ---

    public final void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            for (ListenerSpec<?> spec : listenerSpecs) {
                activeSubscriptions.add(subscribe(spec));
            }
            onEnable();
        } else {
            for (EventBus.Subscription subscription : activeSubscriptions) {
                subscription.unsubscribe();
            }
            activeSubscriptions.clear();
            onDisable();
        }
        if (toggleCallback != null) {
            toggleCallback.run();
        }
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final String id() {
        return id;
    }

    public final String name() {
        return PrimeLang.module(id, "name", name);
    }

    public final String description() {
        return PrimeLang.module(id, "description", description);
    }

    public final ModuleCategory category() {
        return category;
    }

    public final List<Setting> settings() {
        return Collections.unmodifiableList(settings);
    }

    // --- wiring & persistence (ModuleManager only) ---

    final void attach(EventBus eventBus, Runnable toggleCallback) {
        this.eventBus = eventBus;
        this.toggleCallback = toggleCallback;
    }

    final void writeConfig(JsonObject json) {
        json.addProperty("enabled", enabled);
        if (!settings.isEmpty()) {
            JsonObject settingsJson = new JsonObject();
            for (Setting setting : settings) {
                settingsJson.add(setting.id(), setting.toJson());
            }
            json.add("settings", settingsJson);
        }
    }

    final void readConfig(JsonObject json) {
        JsonElement settingsJson = json.get("settings");
        if (settingsJson != null && settingsJson.isJsonObject()) {
            JsonObject settingsObject = settingsJson.getAsJsonObject();
            for (Setting setting : settings) {
                JsonElement stored = settingsObject.get(setting.id());
                if (stored != null) {
                    setting.fromJson(stored);
                }
            }
        }
        JsonElement enabledJson = json.get("enabled");
        setEnabled(enabledJson != null && enabledJson.isJsonPrimitive() && enabledJson.getAsBoolean());
    }

    private <T> EventBus.Subscription subscribe(ListenerSpec<T> spec) {
        return eventBus.subscribe(spec.eventType(), spec.priority(), spec.handler());
    }
}
