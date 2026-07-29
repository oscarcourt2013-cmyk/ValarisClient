package dev.stellarclient.core.gui.animation;

import dev.stellarclient.core.util.Easing;

import java.util.HashMap;
import java.util.Map;

/** Keyed float animations for UI transitions (fade, slide, scale, hover). */
public final class UiAnimator {

    private final Map<String, Float> values = new HashMap<>();
    private final Map<String, Float> targets = new HashMap<>();

    public float get(String key, float defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public void set(String key, float value) {
        values.put(key, value);
        targets.put(key, value);
    }

    public void target(String key, float target) {
        targets.put(key, target);
    }

    public void tick(String key, float deltaSeconds, float speed) {
        float current = values.getOrDefault(key, targets.getOrDefault(key, 0f));
        float target = targets.getOrDefault(key, current);
        values.put(key, Easing.lerp(current, target, deltaSeconds * speed));
    }

    public void tickAll(float deltaSeconds, float speed) {
        for (String key : targets.keySet()) {
            tick(key, deltaSeconds, speed);
        }
    }

    public boolean isSettled(String key, float epsilon) {
        float current = values.getOrDefault(key, 0f);
        float target = targets.getOrDefault(key, 0f);
        return Math.abs(current - target) <= epsilon;
    }
}
