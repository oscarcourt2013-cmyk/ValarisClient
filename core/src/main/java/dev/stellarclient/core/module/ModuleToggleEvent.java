package dev.stellarclient.core.module;

/**
 * Posted on the event bus whenever a module is toggled (keybind, GUI or
 * config load). Allocation on toggle only â€” never in a hot path.
 */
public record ModuleToggleEvent(Module module, boolean enabled) {
}
