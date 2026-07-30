package dev.valerisclient.core.notification;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.valerisclient.core.config.ConfigBinding;
import dev.valerisclient.core.hud.HudAnchor;

/** Notification placement and animation preferences. */
public final class NotificationPreferences implements ConfigBinding {

    private HudAnchor anchor = HudAnchor.TOP_RIGHT;
    private float slideStrength = 1f;
    private boolean showIcons = true;
    private boolean moduleToggleNotifs = true;

    public HudAnchor anchor() {
        return anchor;
    }

    public float slideStrength() {
        return slideStrength;
    }

    public boolean showIcons() {
        return showIcons;
    }

    public boolean moduleToggleNotifs() {
        return moduleToggleNotifs;
    }

    public void setModuleToggleNotifs(boolean moduleToggleNotifs) {
        this.moduleToggleNotifs = moduleToggleNotifs;
    }

    public void cycleAnchor() {
        HudAnchor[] values = HudAnchor.values();
        anchor = values[(anchor.ordinal() + 1) % values.length];
    }

    @Override
    public String configKey() {
        return "notifications";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("anchor", anchor.name());
        json.addProperty("slide", slideStrength);
        json.addProperty("icons", showIcons);
        json.addProperty("moduleToggles", moduleToggleNotifs);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("anchor")) {
            try {
                anchor = HudAnchor.valueOf(json.get("anchor").getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (json.has("slide")) {
            slideStrength = json.get("slide").getAsFloat();
        }
        if (json.has("icons")) {
            showIcons = json.get("icons").getAsBoolean();
        }
        if (json.has("moduleToggles")) {
            moduleToggleNotifs = json.get("moduleToggles").getAsBoolean();
        }
    }
}
