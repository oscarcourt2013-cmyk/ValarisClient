package dev.valarisclient.core.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * Packed ARGB color (0xAARRGGBB), serialized as {@code "#AARRGGBB"} so
 * config files stay hand-editable.
 */
public final class ColorSetting extends Setting {

    private final int defaultArgb;
    private int argb;

    public ColorSetting(String id, String name, String description, int defaultArgb) {
        super(id, name, description);
        this.defaultArgb = defaultArgb;
        this.argb = defaultArgb;
    }

    public int get() {
        return argb;
    }

    public void set(int argb) {
        this.argb = argb;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(String.format("#%08X", argb));
    }

    @Override
    public void fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return;
        }
        String text = element.getAsString();
        if (text.length() == 9 && text.charAt(0) == '#') {
            try {
                this.argb = (int) Long.parseLong(text.substring(1), 16);
            } catch (NumberFormatException ignored) {
                // keep current value
            }
        }
    }

    @Override
    public void reset() {
        this.argb = defaultArgb;
    }
}
