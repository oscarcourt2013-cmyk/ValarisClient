package dev.valarisclient.core.crosshair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.valarisclient.core.config.ConfigBinding;

import java.util.LinkedHashMap;
import java.util.Map;

/** Per-server crosshair profiles keyed by server address. */
public final class CrosshairProfileManager implements ConfigBinding {

    private final Map<String, CrosshairConfig> byServer = new LinkedHashMap<>();
    private final CrosshairConfig global;

    public CrosshairProfileManager(CrosshairConfig global) {
        this.global = global;
    }

    public void applyForServer(String serverKey) {
        String key = normalize(serverKey);
        CrosshairConfig profile = byServer.get(key);
        if (profile != null) {
            global.loadConfig(profile.saveConfig());
            global.serverProfile = key;
        } else {
            global.serverProfile = "global";
        }
    }

    public void saveCurrentForServer(String serverKey) {
        String key = normalize(serverKey);
        CrosshairConfig copy = new CrosshairConfig();
        copy.loadConfig(global.saveConfig());
        copy.serverProfile = key;
        byServer.put(key, copy);
    }

    public void exportJson(JsonObject out) {
        for (Map.Entry<String, CrosshairConfig> e : byServer.entrySet()) {
            out.add(e.getKey(), e.getValue().saveConfig());
        }
    }

    public void importJson(JsonObject in) {
        byServer.clear();
        for (Map.Entry<String, JsonElement> e : in.entrySet()) {
            CrosshairConfig cfg = new CrosshairConfig();
            cfg.loadConfig(e.getValue());
            byServer.put(e.getKey(), cfg);
        }
    }

    private static String normalize(String server) {
        if (server == null || server.isBlank() || "Singleplayer".equals(server)) {
            return "singleplayer";
        }
        return server.toLowerCase().replace(':', '_');
    }

    @Override
    public String configKey() {
        return "crosshair-profiles";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        exportJson(json);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            importJson(element.getAsJsonObject());
        }
    }
}
