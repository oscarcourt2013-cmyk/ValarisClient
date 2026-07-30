package dev.stellarclient.core.module;

import com.google.gson.JsonElement;
import dev.stellarclient.core.i18n.PrimeLang;

/**
 * A user-configurable module setting.
 *
 * <p>Sealed hierarchy: the GUI renders each setting with an exhaustive
 * pattern-matching switch, and every subclass stores its value as a primitive
 * field — reading a setting in a render/tick path never boxes.</p>
 *
 * <p>{@code fromJson} implementations are tolerant: invalid or missing data
 * keeps the current value, so old configs never crash a newer client.</p>
 */
public abstract sealed class Setting
        permits BooleanSetting, IntSetting, DoubleSetting, EnumSetting, ColorSetting, StringSetting {

    private final String id;
    private final String name;
    private final String description;
    private String moduleId;

    protected Setting(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    final void bindModule(String moduleId) {
        this.moduleId = moduleId;
    }

    /** Stable identifier used in config files. */
    public final String id() {
        return id;
    }

    public final String name() {
        if (moduleId != null) {
            return PrimeLang.setting(moduleId, id, "name", name);
        }
        return name;
    }

    public final String description() {
        if (moduleId != null) {
            return PrimeLang.setting(moduleId, id, "description", description);
        }
        return description;
    }

    /** Serializes the current value. */
    public abstract JsonElement toJson();

    /** Restores a value; silently keeps the current one on invalid data. */
    public abstract void fromJson(JsonElement element);

    /** Restores the default value. */
    public abstract void reset();
}
