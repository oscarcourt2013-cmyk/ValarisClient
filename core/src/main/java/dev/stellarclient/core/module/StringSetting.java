package dev.stellarclient.core.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Free text (messages, formats, server addresses...). */
public final class StringSetting extends Setting {

    private final String defaultValue;
    private String value;

    public StringSetting(String id, String name, String description, String defaultValue) {
        super(id, name, description);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public String get() {
        return value;
    }

    public void set(String value) {
        this.value = value != null ? value : defaultValue;
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            this.value = element.getAsString();
        }
    }

    @Override
    public void reset() {
        this.value = defaultValue;
    }
}
