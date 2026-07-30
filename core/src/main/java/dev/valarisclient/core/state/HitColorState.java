package dev.valarisclient.core.state;

/** Shared hit-overlay color, read by version-layer mixins. */
public final class HitColorState {

    private static boolean active;
    private static int argb = 0x80FF0000;

    private HitColorState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static int argb() {
        return argb;
    }

    public static void setArgb(int value) {
        argb = value;
    }

    public static void reset() {
        active = false;
        argb = 0x80FF0000;
    }
}
