package dev.stellarclient.core.util;

/** Cardinal direction from player yaw. Allocation-free. */
public final class DirectionUtil {

    private DirectionUtil() {
    }

    public static String fromYaw(float yaw) {
        float normalized = (yaw % 360 + 360) % 360;
        if (normalized >= 315 || normalized < 45) {
            return "S";
        }
        if (normalized < 135) {
            return "W";
        }
        if (normalized < 225) {
            return "N";
        }
        return "E";
    }
}
