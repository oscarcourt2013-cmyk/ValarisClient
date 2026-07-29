package dev.stellarclient.core.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Choice between the constants of an enum (mode selectors). */
public final class EnumSetting<E extends Enum<E>> extends Setting {

    private final E defaultValue;
    private final E[] values;
    private E value;

    public EnumSetting(String id, String name, String description, E defaultValue) {
        super(id, name, description);
        this.defaultValue = defaultValue;
        this.values = defaultValue.getDeclaringClass().getEnumConstants();
        this.value = defaultValue;
    }

    public E get() {
        return value;
    }

    public void set(E value) {
        this.value = value;
    }

    /** Cycles to the next constant â€” one-click toggling in the GUI. */
    public void cycle() {
        this.value = values[(value.ordinal() + 1) % values.length];
    }

    /** All selectable constants, for the GUI. */
    public E[] values() {
        return values.clone();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value.name());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return;
        }
        String name = element.getAsString();
        for (E candidate : values) {
            if (candidate.name().equals(name)) {
                this.value = candidate;
                return;
            }
        }
    }

    @Override
    public void reset() {
        this.value = defaultValue;
    }
}
