package dev.stellarclient.core.cosmetics;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight cloth-like cape pose boosts â€” pure math, no mesh soft-body.
 * Modulates vanilla {@code capeFlap}/{@code capeLean}/{@code capeLean2} from movement.
 */
public final class CapePhysics {

    /** Vanilla-ish clamps, slightly widened for jump flare. */
    private static final float FLAP_MIN = -8f;
    private static final float FLAP_MAX = 40f;
    private static final float LEAN_MIN = 0f;
    private static final float LEAN_MAX = 160f;
    private static final float LEAN2_MIN = -24f;
    private static final float LEAN2_MAX = 24f;

    /** Exponential smoothing toward boost targets (per render extract). */
    private static final float SMOOTH = 0.38f;

    private static final Map<UUID, Boost> smoothed = new ConcurrentHashMap<>();

    private CapePhysics() {
    }

    /** Motion sample in blocks/tick (Minecraft delta movement). */
    public record Motion(
            float horizontalSpeed,
            float verticalVelocity,
            boolean onGround,
            boolean sprinting,
            float ageInTicks) {
    }

    /** Absolute cape pose after enhancement. */
    public record Pose(float flap, float lean, float lean2) {
    }

    /** Additive boosts before clamping. */
    public record Boost(float flap, float lean, float lean2) {
        static final Boost ZERO = new Boost(0f, 0f, 0f);
    }

    /**
     * Target boosts from motion â€” hang when rising / idle, flare when falling / sprinting,
     * subtle idle sway. Cheap trig only; no allocations beyond the return record.
     */
    public static Boost computeBoost(Motion motion, float intensity) {
        float i = Math.clamp(intensity, 0f, 1.5f);
        if (i <= 0f || motion == null) {
            return Boost.ZERO;
        }

        float hs = Math.clamp(motion.horizontalSpeed(), 0f, 1.2f);
        float vy = motion.verticalVelocity();
        float age = motion.ageInTicks();

        float airFlap;
        if (!motion.onGround()) {
            // Falling (vy < 0) flares cape out; rising tucks it toward the back.
            airFlap = Math.clamp(-vy * 48f, -10f, 26f);
            if (vy > 0.05f) {
                airFlap -= 4f;
            }
        } else {
            // Grounded: slight hang when almost still.
            airFlap = -2.2f * (1f - Math.min(hs / 0.22f, 1f));
        }

        float speedLean = hs * (motion.sprinting() ? 32f : 18f);
        if (!motion.onGround()) {
            speedLean *= 1.15f;
        }

        float still = 1f - Math.min(hs / 0.25f, 1f);
        float sway = (float) Math.sin(age * 0.09) * 2.4f * still;
        float sway2 = (float) Math.sin(age * 0.07 + 1.7) * 1.6f * still;

        return new Boost(
                (airFlap + sway) * i,
                (speedLean + Math.abs(sway) * 0.45f) * i,
                sway2 * i);
    }

    /** Stateless enhance: vanilla pose + boost, clamped. */
    public static Pose enhance(float flap, float lean, float lean2, Motion motion, float intensity) {
        return apply(flap, lean, lean2, computeBoost(motion, intensity));
    }

    /**
     * Smooth boosts per entity so jump/sprint transitions feel cloth-like without soft-body cost.
     */
    public static Pose smooth(
            UUID entityId,
            float flap,
            float lean,
            float lean2,
            Motion motion,
            float intensity) {
        Boost target = computeBoost(motion, intensity);
        if (entityId == null) {
            return apply(flap, lean, lean2, target);
        }
        Boost prev = smoothed.getOrDefault(entityId, Boost.ZERO);
        Boost next = new Boost(
                lerp(prev.flap(), target.flap(), SMOOTH),
                lerp(prev.lean(), target.lean(), SMOOTH),
                lerp(prev.lean2(), target.lean2(), SMOOTH));
        smoothed.put(entityId, next);
        return apply(flap, lean, lean2, next);
    }

    public static void clear(UUID entityId) {
        if (entityId != null) {
            smoothed.remove(entityId);
        }
    }

    public static void clearAll() {
        smoothed.clear();
    }

    private static Pose apply(float flap, float lean, float lean2, Boost boost) {
        return new Pose(
                Math.clamp(flap + boost.flap(), FLAP_MIN, FLAP_MAX),
                Math.clamp(lean + boost.lean(), LEAN_MIN, LEAN_MAX),
                Math.clamp(lean2 + boost.lean2(), LEAN2_MIN, LEAN2_MAX));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }
}
