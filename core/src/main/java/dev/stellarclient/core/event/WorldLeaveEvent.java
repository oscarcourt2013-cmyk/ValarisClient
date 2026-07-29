package dev.stellarclient.core.event;

/** Fired when the player leaves a world or disconnects. Stateless singleton. */
public final class WorldLeaveEvent {

    public static final WorldLeaveEvent INSTANCE = new WorldLeaveEvent();

    private WorldLeaveEvent() {
    }
}
