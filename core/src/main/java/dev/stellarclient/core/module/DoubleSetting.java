package dev.stellarclient.core.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Floating-point slider, clamped to [min, max]. */
public final class DoubleSetting extends Setting {

    private final double defaultValue;
    private final double min;
    private final double max;
    private double value;

    public DoubleSetting(String id, String name, String description, double defaultValue, double min, double max) {
        super(id, name, description);
        if (min > max || defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException(id + ": default " + defaultValue + " outside [" + min + ", " + max + "]");
        }
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.value = defaultValue;
    }

    public double get() {
        return value;
    }

    public void set(double value) {
        this.value = Math.clamp(value, min, max);
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            set(element.getAsDouble());
        }
    }

    @Override
    public void reset() {
        this.value = defaultValue;
    }
}
