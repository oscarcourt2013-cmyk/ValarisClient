package dev.stellarclient.core.voice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.config.ConfigBinding;

/** Persisted Prime Voice settings. */
public final class VoiceChatSettings implements ConfigBinding {

    /** Default relay â€” Prime Voice on production Ptero node. */
    public static final String DEFAULT_RELAY = "ws://194.9.172.102:26005/voice";

    /** Simple Voice Chat default proximity radius. */
    public static final int DEFAULT_PROXIMITY = 48;

    private String relayUrl = DEFAULT_RELAY;
    private boolean pushToTalk = true;
    private int inputVolume = 100;
    private int outputVolume = 100;
    private int proximityBlocks = DEFAULT_PROXIMITY;
    private boolean proximityEnabled = true;
    private boolean showHud = true;
    private VoiceListenMode listenMode = VoiceListenMode.BOTH;
    private String activeGroupId = "";
    private String activeGroupName = "";

    public String relayUrl() {
        return relayUrl == null || relayUrl.isBlank() ? DEFAULT_RELAY : relayUrl;
    }

    public void setRelayUrl(String relayUrl) {
        this.relayUrl = relayUrl;
    }

    public boolean pushToTalk() {
        return pushToTalk;
    }

    public void setPushToTalk(boolean pushToTalk) {
        this.pushToTalk = pushToTalk;
    }

    public int inputVolume() {
        return Math.clamp(inputVolume, 0, 200);
    }

    public void setInputVolume(int inputVolume) {
        this.inputVolume = inputVolume;
    }

    public int outputVolume() {
        return Math.clamp(outputVolume, 0, 200);
    }

    public void setOutputVolume(int outputVolume) {
        this.outputVolume = outputVolume;
    }

    public int proximityBlocks() {
        return Math.clamp(proximityBlocks, 8, 128);
    }

    public void setProximityBlocks(int proximityBlocks) {
        this.proximityBlocks = proximityBlocks;
    }

    public boolean proximityEnabled() {
        return proximityEnabled;
    }

    public void setProximityEnabled(boolean proximityEnabled) {
        this.proximityEnabled = proximityEnabled;
    }

    public boolean showHud() {
        return showHud;
    }

    public void setShowHud(boolean showHud) {
        this.showHud = showHud;
    }

    public VoiceListenMode listenMode() {
        return listenMode == null ? VoiceListenMode.BOTH : listenMode;
    }

    public void setListenMode(VoiceListenMode listenMode) {
        this.listenMode = listenMode == null ? VoiceListenMode.BOTH : listenMode;
    }

    public String activeGroupId() {
        return activeGroupId == null ? "" : activeGroupId;
    }

    public void setActiveGroupId(String activeGroupId) {
        this.activeGroupId = activeGroupId == null ? "" : activeGroupId.trim();
    }

    public String activeGroupName() {
        return activeGroupName == null ? "" : activeGroupName;
    }

    public void setActiveGroupName(String activeGroupName) {
        this.activeGroupName = activeGroupName == null ? "" : activeGroupName.trim();
    }

    public boolean inGroup() {
        return !activeGroupId().isBlank();
    }

    @Override
    public String configKey() {
        return "voice-chat";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("relayUrl", relayUrl);
        json.addProperty("pushToTalk", pushToTalk);
        json.addProperty("inputVolume", inputVolume);
        json.addProperty("outputVolume", outputVolume);
        json.addProperty("proximityBlocks", proximityBlocks);
        json.addProperty("proximityEnabled", proximityEnabled);
        json.addProperty("showHud", showHud);
        json.addProperty("listenMode", listenMode.name());
        json.addProperty("activeGroupId", activeGroupId);
        json.addProperty("activeGroupName", activeGroupName);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("relayUrl")) relayUrl = json.get("relayUrl").getAsString();
        if (json.has("pushToTalk")) pushToTalk = json.get("pushToTalk").getAsBoolean();
        if (json.has("inputVolume")) inputVolume = json.get("inputVolume").getAsInt();
        if (json.has("outputVolume")) outputVolume = json.get("outputVolume").getAsInt();
        if (json.has("proximityBlocks")) proximityBlocks = json.get("proximityBlocks").getAsInt();
        if (json.has("proximityEnabled")) proximityEnabled = json.get("proximityEnabled").getAsBoolean();
        if (json.has("showHud")) showHud = json.get("showHud").getAsBoolean();
        if (json.has("listenMode")) {
            try {
                listenMode = VoiceListenMode.valueOf(json.get("listenMode").getAsString());
            } catch (IllegalArgumentException ignored) {
                listenMode = VoiceListenMode.BOTH;
            }
        }
        if (json.has("activeGroupId")) activeGroupId = json.get("activeGroupId").getAsString();
        if (json.has("activeGroupName")) activeGroupName = json.get("activeGroupName").getAsString();
    }
}
