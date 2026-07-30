package dev.valarisclient.core.state;

/** First-person hand tint, read by version-layer item-in-hand mixins. */
public final class HandShaderState {

    private static boolean active;
    private static int argb = 0x80FFFFFF;
    private static float intensity = 0.35f;

    private HandShaderState() {
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

    public static float intensity() {
        return intensity;
    }

    public static void setIntensity(float value) {
        intensity = Math.clamp(value, 0f, 1f);
    }

    /** Multiplicative RGB factors for first-person tint overlays. */
    public static float red() {
        return mixChannel((argb >> 16) & 0xFF);
    }

    public static float green() {
        return mixChannel((argb >> 8) & 0xFF);
    }

    public static float blue() {
        return mixChannel(argb & 0xFF);
    }

    private static float mixChannel(int channel) {
        float tint = channel / 255f;
        return 1f - intensity + tint * intensity;
    }

    public static void reset() {
        active = false;
        argb = 0x80FFFFFF;
        intensity = 0.35f;
    }
}
