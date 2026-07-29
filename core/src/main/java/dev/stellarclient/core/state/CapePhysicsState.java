package dev.stellarclient.core.state;

import dev.stellarclient.core.cosmetics.CapePhysics;

/** Toggles cloth-like cape pose boosts for equipped Prime capes. */
public final class CapePhysicsState {

    private static boolean active = true;
    private static float intensity = 1f;

    private CapePhysicsState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
        if (!value) {
            CapePhysics.clearAll();
        }
    }

    public static float intensity() {
        return intensity;
    }

    public static void setIntensity(float value) {
        intensity = Math.clamp(value, 0f, 1.5f);
    }

    public static void reset() {
        active = true;
        intensity = 1f;
        CapePhysics.clearAll();
    }
}
