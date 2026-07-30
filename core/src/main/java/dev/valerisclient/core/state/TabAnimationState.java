package dev.valerisclient.core.state;

/** Subtle tab-list open animation, read by PlayerTabOverlay mixins. */
public final class TabAnimationState {

    private static boolean active;
    private static int durationMs = 160;
    private static float slidePixels = 6f;

    private TabAnimationState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static int durationMs() {
        return durationMs;
    }

    public static void setDurationMs(int value) {
        durationMs = Math.clamp(value, 40, 400);
    }

    public static float slidePixels() {
        return slidePixels;
    }

    public static void setSlidePixels(float value) {
        slidePixels = Math.clamp(value, 0f, 16f);
    }

    public static void reset() {
        active = false;
        durationMs = 160;
        slidePixels = 6f;
    }
}
