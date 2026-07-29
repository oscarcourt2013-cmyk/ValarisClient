package dev.stellarclient.core.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.stellarclient.core.config.ConfigBinding;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Pinned modules for quick access in the ClickGUI. */
public final class FavoritesManager implements ConfigBinding {

    private final LinkedHashSet<String> favoriteIds = new LinkedHashSet<>();

    public boolean isFavorite(String moduleId) {
        return favoriteIds.contains(moduleId);
    }

    public void toggle(String moduleId) {
        if (favoriteIds.contains(moduleId)) {
            favoriteIds.remove(moduleId);
        } else {
            favoriteIds.add(moduleId);
        }
    }

    public void add(String moduleId) {
        favoriteIds.add(moduleId);
    }

    public void clear() {
        favoriteIds.clear();
    }

    public List<Module> resolve(ModuleManager modules) {
        List<Module> resolved = new ArrayList<>(favoriteIds.size());
        for (String id : favoriteIds) {
            Module module = modules.get(id);
            if (module != null) {
                resolved.add(module);
            }
        }
        return resolved;
    }

    @Override
    public String configKey() {
        return "favorites";
    }

    @Override
    public JsonElement saveConfig() {
        JsonArray array = new JsonArray();
        for (String id : favoriteIds) {
            array.add(new JsonPrimitive(id));
        }
        return array;
    }

    @Override
    public void loadConfig(JsonElement element) {
        favoriteIds.clear();
        if (element == null || !element.isJsonArray()) {
            return;
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry.isJsonPrimitive()) {
                favoriteIds.add(entry.getAsString());
            }
        }
    }
}
