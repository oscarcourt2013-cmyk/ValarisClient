package dev.stellarclient.core.event;

/** Fired when the local player dies. */
public record PlayerDeathEvent(double x, double y, double z) {
}
