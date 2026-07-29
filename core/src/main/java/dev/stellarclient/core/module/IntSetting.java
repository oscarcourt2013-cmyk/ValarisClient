package dev.stellarclient.core.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Integer slider, clamped to [min, max]. */
public final class IntSetting extends Setting {

    private final int defaultValue;
    private final int min;
    private final int max;
    private int value;

    public IntSetting(String id, String name, String description, int defaultValue, int min, int max) {
        super(id, name, description);
        if (min > max || defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException(id + ": default " + defaultValue + " outside [" + min + ", " + max + "]");
        }
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.value = defaultValue;
    }

    public int get() {
        return value;
    }

    public void set(int value) {
        this.value = Math.clamp(value, min, max);
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            set(element.getAsInt());
        }
    }

    @Override
    public void reset() {
        this.value = defaultValue;
    }
}
