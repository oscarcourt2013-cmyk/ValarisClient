package dev.stellarclient.core.replay;

/** One recorded player sample (event-based, compact). */
public record ReplayFrame(
        long worldTimeMillis,
        double x, double y, double z,
        float yaw, float pitch,
        boolean sprinting,
        boolean sneaking
) {
}
