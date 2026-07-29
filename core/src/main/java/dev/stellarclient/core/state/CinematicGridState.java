package dev.stellarclient.core.state;

/** Rule-of-thirds overlay flag, read by HUD render hooks. */
public final class CinematicGridState {

    private static boolean active;

    private CinematicGridState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
