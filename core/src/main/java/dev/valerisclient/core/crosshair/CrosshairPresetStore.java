package dev.valerisclient.core.crosshair;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.valerisclient.core.config.ConfigBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Named crosshair presets with import/export support. */
public final class CrosshairPresetStore implements ConfigBinding {

    private final Map<String, CrosshairConfig> presets = new LinkedHashMap<>();

    public CrosshairPresetStore() {
        CrosshairConfig classic = new CrosshairConfig();
        classic.style = CrosshairStyle.CLASSIC;
        presets.put("Classic", classic);

        CrosshairConfig dot = new CrosshairConfig();
        dot.style = CrosshairStyle.DOT;
        dot.size = 9;
        dot.armLength = 1;
        presets.put("Dot", dot);

        CrosshairConfig circle = new CrosshairConfig();
        circle.style = CrosshairStyle.CIRCLE;
        circle.armLength = 5;
        circle.gap = 3;
        presets.put("Circle", circle);
    }

    public List<String> names() {
        return new ArrayList<>(presets.keySet());
    }

    public CrosshairConfig get(String name) {
        CrosshairConfig copy = presets.get(name);
        if (copy == null) {
            return null;
        }
        CrosshairConfig cfg = new CrosshairConfig();
        cfg.loadConfig(copy.saveConfig());
        return cfg;
    }

    public void save(String name, CrosshairConfig config) {
        CrosshairConfig copy = new CrosshairConfig();
        copy.loadConfig(config.saveConfig());
        presets.put(name, copy);
    }

    public void applyTo(CrosshairConfig target, String presetName) {
        CrosshairConfig preset = get(presetName);
        if (preset != null) {
            target.loadConfig(preset.saveConfig());
        }
    }

    @Override
    public String configKey() {
        return "crosshair-presets";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, CrosshairConfig> entry : presets.entrySet()) {
            root.add(entry.getKey(), entry.getValue().saveConfig());
        }
        return root;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject root = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            CrosshairConfig cfg = new CrosshairConfig();
            cfg.loadConfig(entry.getValue());
            presets.put(entry.getKey(), cfg);
        }
    }

    public JsonElement exportAll() {
        return saveConfig();
    }

    public void importAll(JsonElement element) {
        loadConfig(element);
    }
}
