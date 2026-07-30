package dev.valerisclient.core.state;

/** Client-side fire overlay offset read by version-layer screen effect mixins. */
public final class LowFireState {

    private static boolean active;
    private static float heightOffset;

    private LowFireState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static float heightOffset() {
        return heightOffset;
    }

    public static void setHeightOffset(float value) {
        heightOffset = Math.max(0.0F, value);
    }
}
