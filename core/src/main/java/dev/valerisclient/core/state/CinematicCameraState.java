package dev.valerisclient.core.state;

/** Smoothed camera rotation, read by version-layer mixins. */
public final class CinematicCameraState {

    private static boolean active;
    private static float yaw;
    private static float pitch;

    private CinematicCameraState() {
    }

    public static boolean active() {
        return active;
    }

    public static float yaw() {
        return yaw;
    }

    public static float pitch() {
        return pitch;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static void setRotation(float yawDegrees, float pitchDegrees) {
        yaw = yawDegrees;
        pitch = Math.clamp(pitchDegrees, -90.0f, 90.0f);
    }

    public static void reset() {
        active = false;
        yaw = 0;
        pitch = 0;
    }
}
