package dev.valarisclient.core.module;

/**
 * Posted on the event bus whenever a module is toggled (keybind, GUI or
 * config load). Allocation on toggle only — never in a hot path.
 */
public record ModuleToggleEvent(Module module, boolean enabled) {
}
