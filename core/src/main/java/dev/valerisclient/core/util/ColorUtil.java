package dev.valerisclient.core.util;

/** Packed ARGB helpers. Allocation-free. */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static int withAlpha(int argb, float opacity) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * Math.clamp(opacity, 0f, 1f));
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    public static int tint(int argb, int tintArgb) {
        if (tintArgb == 0) {
            return argb;
        }
        int r = ((argb >> 16) & 0xFF) * ((tintArgb >> 16) & 0xFF) / 255;
        int g = ((argb >> 8) & 0xFF) * ((tintArgb >> 8) & 0xFF) / 255;
        int b = (argb & 0xFF) * (tintArgb & 0xFF) / 255;
        int a = ((argb >>> 24) & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lerp(int fromArgb, int toArgb, float t) {
        float p = Math.clamp(t, 0f, 1f);
        int af = (fromArgb >>> 24) & 0xFF;
        int rf = (fromArgb >> 16) & 0xFF;
        int gf = (fromArgb >> 8) & 0xFF;
        int bf = fromArgb & 0xFF;
        int at = (toArgb >>> 24) & 0xFF;
        int rt = (toArgb >> 16) & 0xFF;
        int gt = (toArgb >> 8) & 0xFF;
        int bt = toArgb & 0xFF;
        int a = Math.round(af + (at - af) * p);
        int r = Math.round(rf + (rt - rf) * p);
        int g = Math.round(gf + (gt - gf) * p);
        int b = Math.round(bf + (bt - bf) * p);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int fromHsv(float hue, float saturation, float value, float alpha) {
        float h = (hue % 360f + 360f) % 360f / 60f;
        float s = Math.clamp(saturation, 0f, 1f);
        float v = Math.clamp(value, 0f, 1f);
        int i = (int) h;
        float f = h - i;
        float p = v * (1f - s);
        float q = v * (1f - s * f);
        float t = v * (1f - s * (1f - f));
        float r;
        float g;
        float b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        int a = Math.round(Math.clamp(alpha, 0f, 1f) * 255f);
        return (a << 24)
                | (Math.round(r * 255f) << 16)
                | (Math.round(g * 255f) << 8)
                | Math.round(b * 255f);
    }

    public static float[] toHsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta < 1e-5f) {
            hue = 0f;
        } else if (max == r) {
            hue = 60f * (((g - b) / delta) % 6f);
        } else if (max == g) {
            hue = 60f * (((b - r) / delta) + 2f);
        } else {
            hue = 60f * (((r - g) / delta) + 4f);
        }
        if (hue < 0f) {
            hue += 360f;
        }
        float sat = max <= 0f ? 0f : delta / max;
        return new float[]{hue, sat, max, ((argb >>> 24) & 0xFF) / 255f};
    }

    public static String toHex(int argb) {
        return String.format("#%08X", argb);
    }

    public static int parseHex(String text, int fallback) {
        if (text == null || text.length() != 9 || text.charAt(0) != '#') {
            return fallback;
        }
        try {
            return (int) Long.parseLong(text.substring(1), 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
