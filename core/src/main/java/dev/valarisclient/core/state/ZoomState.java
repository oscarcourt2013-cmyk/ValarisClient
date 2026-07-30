package dev.valarisclient.core.state;

/** Shared zoom FOV multiplier, read by version-layer mixins. */
public final class ZoomState {

    private static float multiplier = 1.0f;

    private ZoomState() {
    }

    public static float multiplier() {
        return multiplier;
    }

    public static void setMultiplier(float value) {
        multiplier = Math.clamp(value, 0.05f, 1.0f);
    }

    public static void reset() {
        multiplier = 1.0f;
    }
}
