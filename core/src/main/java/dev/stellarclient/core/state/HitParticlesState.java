package dev.stellarclient.core.state;

/** Hit-only particle overrides (does not touch world particles). */
public final class HitParticlesState {

    public enum Preset {
        BLOOD(0xFFFF3333, 6, 1.0f, 12),
        SPARK(0xFFFFFFAA, 8, 0.8f, 8),
        CRITICAL(0xFFFFD700, 10, 1.2f, 10),
        MAGIC(0xFFAA55FF, 7, 1.0f, 14),
        FIRE(0xFFFF6600, 9, 1.1f, 10);

        public final int color;
        public final int count;
        public final float size;
        public final int lifetimeTicks;

        Preset(int color, int count, float size, int lifetimeTicks) {
            this.color = color;
            this.count = count;
            this.size = size;
            this.lifetimeTicks = lifetimeTicks;
        }
    }

    private static boolean active;
    private static Preset preset = Preset.SPARK;
    private static float intensity = 1f;

    private HitParticlesState() {
    }

    public static void configure(boolean enabled, Preset selected, float amount) {
        active = enabled;
        preset = selected;
        intensity = Math.clamp(amount, 0f, 2f);
    }

    public static boolean active() {
        return active;
    }

    public static Preset preset() {
        return preset;
    }

    public static int scaledCount() {
        return Math.max(1, Math.round(preset.count * intensity));
    }

    public static float size() {
        return preset.size;
    }

    public static int color() {
        return preset.color;
    }

    public static int lifetimeTicks() {
        return preset.lifetimeTicks;
    }

    public static void reset() {
        active = false;
        preset = Preset.SPARK;
        intensity = 1f;
    }
}
