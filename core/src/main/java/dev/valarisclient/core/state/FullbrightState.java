package dev.valarisclient.core.state;

/** Fullbright flag read by version-layer lightmap mixins. */
public final class FullbrightState {

    private static boolean active;

    private FullbrightState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
