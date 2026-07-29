package dev.stellarclient.core.event;

/** Fired when the player joins a world or server. Stateless singleton. */
public final class WorldJoinEvent {

    public static final WorldJoinEvent INSTANCE = new WorldJoinEvent();

    private WorldJoinEvent() {
    }
}
