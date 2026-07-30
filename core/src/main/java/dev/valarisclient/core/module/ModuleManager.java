package dev.valarisclient.core.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.valarisclient.core.config.ConfigBinding;
import dev.valarisclient.core.event.EventBus;
import dev.valarisclient.core.keybind.Keybind;
import dev.valarisclient.core.keybind.KeybindManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of all ValarisClient modules.
 *
 * <p>Registering a module wires everything in one call: event bus injection,
 * a rebindable toggle keybind ({@code module.<id>}, unbound by default), a
 * {@link ModuleToggleEvent} on every toggle, and config persistence — settings
 * saved by an earlier registration order or client version are applied the
 * moment the module registers.</p>
 */
public final class ModuleManager implements ConfigBinding {

    private final EventBus eventBus;
    private final KeybindManager keybinds;

    private final Map<String, Module> byId = new LinkedHashMap<>();
    private final Map<ModuleCategory, List<Module>> byCategory = new EnumMap<>(ModuleCategory.class);

    /** Config sections loaded before their module registered (see {@link #register}). */
    private JsonObject pendingConfig;

    public ModuleManager(EventBus eventBus, KeybindManager keybinds) {
        this.eventBus = eventBus;
        this.keybinds = keybinds;
        for (ModuleCategory category : ModuleCategory.values()) {
            byCategory.put(category, new ArrayList<>());
        }
    }

    /** Registers a module and returns it. */
    public <M extends Module> M register(M module) {
        Module previous = byId.putIfAbsent(module.id(), module);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate module id: " + module.id());
        }
        byCategory.get(module.category()).add(module);

        module.attach(eventBus, () -> eventBus.post(new ModuleToggleEvent(module, module.isEnabled())));
        keybinds.register(new Keybind(
                "module." + module.id(),
                module.name(),
                module.category().displayName(),
                Keybind.UNBOUND
        ).onPress(module::toggle));

        // A config loaded before this module existed still applies to it.
        if (pendingConfig != null) {
            JsonElement section = pendingConfig.get(module.id());
            if (section != null && section.isJsonObject()) {
                module.readConfig(section.getAsJsonObject());
            }
        }
        return module;
    }

    public Module get(String id) {
        return byId.get(id);
    }

    public Collection<Module> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    /** Modules of one category, in registration order. For the ClickGUI. */
    public List<Module> byCategory(ModuleCategory category) {
        return Collections.unmodifiableList(byCategory.get(category));
    }

    /** Case-insensitive search over name and description. For the GUI search bar. */
    public List<Module> search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        List<Module> matches = new ArrayList<>();
        for (Module module : byId.values()) {
            if (module.name().toLowerCase(Locale.ROOT).contains(needle)
                    || module.description().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(module);
            }
        }
        return matches;
    }

    @Override
    public String configKey() {
        return "modules";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (Module module : byId.values()) {
            JsonObject section = new JsonObject();
            module.writeConfig(section);
            json.add(module.id(), section);
        }
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        JsonObject json = element.getAsJsonObject();
        this.pendingConfig = json;
        for (Module module : byId.values()) {
            JsonElement section = json.get(module.id());
            if (section != null && section.isJsonObject()) {
                module.readConfig(section.getAsJsonObject());
            }
        }
    }
}
