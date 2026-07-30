package dev.valarisclient.core.util;

/** Simple easing helpers for GUI animations. */
public final class Easing {

    private Easing() {
    }

    public static float lerp(float from, float to, float progress) {
        return from + (to - from) * Math.clamp(progress, 0f, 1f);
    }

    public static float easeOutCubic(float progress) {
        float t = Math.clamp(progress, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    public static float easeOutBack(float progress) {
        float t = Math.clamp(progress, 0f, 1f);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float inv = t - 1f;
        return 1f + c3 * inv * inv * inv + c1 * inv * inv;
    }

    public static float easeInOutQuad(float progress) {
        float t = Math.clamp(progress, 0f, 1f);
        return t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
    }
}
