package dev.stellarclient.core.state;

/** Client-side daytime override read by version-layer level mixins. */
public final class AlwaysDayState {

    /** Minecraft noon (12:00) in day-time ticks. */
    public static final long NOON_TICKS = 6000L;

    private static boolean active;

    private AlwaysDayState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
