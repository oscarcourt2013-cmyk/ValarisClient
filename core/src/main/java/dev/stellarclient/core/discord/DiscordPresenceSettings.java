package dev.stellarclient.core.discord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.config.ConfigBinding;

/** Persisted Discord Rich Presence display options. */
public final class DiscordPresenceSettings implements ConfigBinding {

    private boolean showServerIp = true;
    private boolean showHealth = true;
    private boolean showPing = true;
    private boolean showBiome = false;
    private boolean showCoordinates = false;
    private boolean showHeldItem = false;
    private boolean showModuleCount = false;
    private boolean showSessionTime = true;
    private boolean showAccountTier = false;
    private boolean showFps = false;
    private int updateIntervalTicks = 40;

    public boolean showServerIp() {
        return showServerIp;
    }

    public void setShowServerIp(boolean showServerIp) {
        this.showServerIp = showServerIp;
    }

    public boolean showHealth() {
        return showHealth;
    }

    public void setShowHealth(boolean showHealth) {
        this.showHealth = showHealth;
    }

    public boolean showPing() {
        return showPing;
    }

    public void setShowPing(boolean showPing) {
        this.showPing = showPing;
    }

    public boolean showBiome() {
        return showBiome;
    }

    public void setShowBiome(boolean showBiome) {
        this.showBiome = showBiome;
    }

    public boolean showCoordinates() {
        return showCoordinates;
    }

    public void setShowCoordinates(boolean showCoordinates) {
        this.showCoordinates = showCoordinates;
    }

    public boolean showHeldItem() {
        return showHeldItem;
    }

    public void setShowHeldItem(boolean showHeldItem) {
        this.showHeldItem = showHeldItem;
    }

    public boolean showModuleCount() {
        return showModuleCount;
    }

    public void setShowModuleCount(boolean showModuleCount) {
        this.showModuleCount = showModuleCount;
    }

    public boolean showSessionTime() {
        return showSessionTime;
    }

    public void setShowSessionTime(boolean showSessionTime) {
        this.showSessionTime = showSessionTime;
    }

    public boolean showAccountTier() {
        return showAccountTier;
    }

    public void setShowAccountTier(boolean showAccountTier) {
        this.showAccountTier = showAccountTier;
    }

    public boolean showFps() {
        return showFps;
    }

    public void setShowFps(boolean showFps) {
        this.showFps = showFps;
    }

    public int updateIntervalTicks() {
        return Math.max(20, updateIntervalTicks);
    }

    public void setUpdateIntervalTicks(int updateIntervalTicks) {
        this.updateIntervalTicks = updateIntervalTicks;
    }

    @Override
    public String configKey() {
        return "discord-rpc";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("showServerIp", showServerIp);
        json.addProperty("showHealth", showHealth);
        json.addProperty("showPing", showPing);
        json.addProperty("showBiome", showBiome);
        json.addProperty("showCoordinates", showCoordinates);
        json.addProperty("showHeldItem", showHeldItem);
        json.addProperty("showModuleCount", showModuleCount);
        json.addProperty("showSessionTime", showSessionTime);
        json.addProperty("showAccountTier", showAccountTier);
        json.addProperty("showFps", showFps);
        json.addProperty("updateIntervalTicks", updateIntervalTicks);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("showServerIp")) showServerIp = json.get("showServerIp").getAsBoolean();
        if (json.has("showHealth")) showHealth = json.get("showHealth").getAsBoolean();
        if (json.has("showPing")) showPing = json.get("showPing").getAsBoolean();
        if (json.has("showBiome")) showBiome = json.get("showBiome").getAsBoolean();
        if (json.has("showCoordinates")) showCoordinates = json.get("showCoordinates").getAsBoolean();
        if (json.has("showHeldItem")) showHeldItem = json.get("showHeldItem").getAsBoolean();
        if (json.has("showModuleCount")) showModuleCount = json.get("showModuleCount").getAsBoolean();
        if (json.has("showSessionTime")) showSessionTime = json.get("showSessionTime").getAsBoolean();
        if (json.has("showAccountTier")) showAccountTier = json.get("showAccountTier").getAsBoolean();
        if (json.has("showFps")) showFps = json.get("showFps").getAsBoolean();
        if (json.has("updateIntervalTicks")) updateIntervalTicks = json.get("updateIntervalTicks").getAsInt();
    }
}
