package dev.valarisclient.core.event;

/** Fired when the local player attacks an entity. */
public record AttackEntityEvent(String targetName) {
}
