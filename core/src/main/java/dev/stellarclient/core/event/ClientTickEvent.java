package dev.stellarclient.core.event;

/**
 * Fired at the end of every client tick (20/s).
 *
 * <p>Carries no state, so a single shared instance is posted â€” zero
 * allocation per tick.</p>
 */
public final class ClientTickEvent {

    public static final ClientTickEvent INSTANCE = new ClientTickEvent();

    private ClientTickEvent() {
    }
}
