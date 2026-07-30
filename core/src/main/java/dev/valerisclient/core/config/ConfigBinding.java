package dev.valerisclient.core.config;

import com.google.gson.JsonElement;

/**
 * A component whose state is persisted inside ValerisClient's JSON configs.
 *
 * <p>Managers (keybinds, themes, modules, HUD layouts, ...) implement this and
 * register themselves with the {@link ConfigManager}. Each binding owns one
 * top-level key in the config file.</p>
 */
public interface ConfigBinding {

    /** Unique top-level key in the config file, e.g. {@code "keybinds"}. */
    String configKey();

    /** Serializes the current state. Must not return {@code null}. */
    JsonElement saveConfig();

    /**
     * Restores state from a previously saved element. Implementations must
     * tolerate missing or partial data (treat it as defaults) — configs
     * written by older client versions stay loadable.
     */
    void loadConfig(JsonElement element);
}
