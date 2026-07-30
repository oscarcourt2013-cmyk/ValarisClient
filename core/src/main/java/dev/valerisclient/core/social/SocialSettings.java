package dev.valerisclient.core.social;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.valerisclient.core.config.ConfigBinding;

/** Social backend settings (same host as voice by default). */
public final class SocialSettings implements ConfigBinding {

    public static final String DEFAULT_API = "http://194.9.172.102:26005";

    private String apiBase = DEFAULT_API;
    private boolean enabled = true;

    public String apiBase() {
        return apiBase == null || apiBase.isBlank() ? DEFAULT_API : apiBase.replaceAll("/$", "");
    }

    public void setApiBase(String apiBase) {
        this.apiBase = apiBase;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String socialWsUrl(String token) {
        String ws = apiBase().replaceFirst("^http", "ws");
        String encoded = java.net.URLEncoder.encode(token == null ? "" : token, java.nio.charset.StandardCharsets.UTF_8);
        return ws + "/social?token=" + encoded;
    }

    @Override
    public String configKey() {
        return "social";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("apiBase", apiBase());
        json.addProperty("enabled", enabled);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("apiBase")) {
            apiBase = json.get("apiBase").getAsString();
        }
        if (json.has("enabled")) {
            enabled = json.get("enabled").getAsBoolean();
        }
    }
}
