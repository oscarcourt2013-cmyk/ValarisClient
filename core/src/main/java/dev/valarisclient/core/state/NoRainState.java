package dev.valarisclient.core.state;

/** Client-side precipitation override read by version-layer level mixins. */
public final class NoRainState {

    private static boolean active;

    private NoRainState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }
}
