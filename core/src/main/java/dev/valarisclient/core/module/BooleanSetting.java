package dev.valarisclient.core.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** On/off switch. */
public final class BooleanSetting extends Setting {

    private final boolean defaultValue;
    private boolean value;

    public BooleanSetting(String id, String name, String description, boolean defaultValue) {
        super(id, name, description);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean value) {
        this.value = value;
    }

    public void toggle() {
        this.value = !this.value;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            this.value = element.getAsBoolean();
        }
    }

    @Override
    public void reset() {
        this.value = defaultValue;
    }
}
