package dev.stellarclient.core.crosshair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.config.ConfigBinding;
import dev.stellarclient.core.util.ColorUtil;

/** Full crosshair configuration with server profile support. */
public final class CrosshairConfig implements ConfigBinding {

    public int size = 11;
    public int armLength = 4;
    public int thickness = 1;
    public int gap = 2;
    public int color = 0xFFFFFFFF;
    public float opacity = 1f;
    public float rotation;
    public CrosshairStyle style = CrosshairStyle.CLASSIC;
    public boolean outline = true;
    public boolean hideVanilla = true;
    public String serverProfile = "global";

    public int effectiveColor() {
        return ColorUtil.withAlpha(color, opacity);
    }

    @Override
    public String configKey() {
        return "crosshair";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("size", size);
        json.addProperty("armLength", armLength);
        json.addProperty("thickness", thickness);
        json.addProperty("gap", gap);
        json.addProperty("color", ColorUtil.toHex(color));
        json.addProperty("opacity", opacity);
        json.addProperty("rotation", rotation);
        json.addProperty("style", style.name());
        json.addProperty("outline", outline);
        json.addProperty("hideVanilla", hideVanilla);
        json.addProperty("serverProfile", serverProfile);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("size")) size = json.get("size").getAsInt();
        if (json.has("armLength")) armLength = json.get("armLength").getAsInt();
        if (json.has("thickness")) thickness = json.get("thickness").getAsInt();
        if (json.has("gap")) gap = json.get("gap").getAsInt();
        if (json.has("color")) color = ColorUtil.parseHex(json.get("color").getAsString(), color);
        if (json.has("opacity")) opacity = json.get("opacity").getAsFloat();
        if (json.has("rotation")) rotation = json.get("rotation").getAsFloat();
        if (json.has("style")) {
            try {
                style = CrosshairStyle.valueOf(json.get("style").getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (json.has("outline")) outline = json.get("outline").getAsBoolean();
        if (json.has("hideVanilla")) hideVanilla = json.get("hideVanilla").getAsBoolean();
        if (json.has("serverProfile")) serverProfile = json.get("serverProfile").getAsString();
    }
}
