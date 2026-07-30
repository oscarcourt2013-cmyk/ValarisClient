package dev.valarisclient.core.hud.vanilla;

import dev.valarisclient.core.hud.HudElement;

/** Shared layout math for version-specific vanilla HUD transform mixins. */
public final class VanillaHudTransformHelper {

    private VanillaHudTransformHelper() {
    }

    public static int localWidth(VanillaHudComponent component) {
        if (component == VanillaHudComponent.SCOREBOARD) {
            VanillaHudMeasurements.Bounds bounds = VanillaHudMeasurements.scoreboard();
            if (bounds.valid()) {
                return bounds.width();
            }
        }
        return component.defaultWidth();
    }

    public static int localHeight(VanillaHudComponent component) {
        if (component == VanillaHudComponent.SCOREBOARD) {
            VanillaHudMeasurements.Bounds bounds = VanillaHudMeasurements.scoreboard();
            if (bounds.valid()) {
                return bounds.height();
            }
        }
        return component.defaultHeight();
    }

    public static float defaultX(VanillaHudComponent component, int screenWidth, float width) {
        if (component == VanillaHudComponent.SCOREBOARD) {
            VanillaHudMeasurements.Bounds bounds = VanillaHudMeasurements.scoreboard();
            if (bounds.valid()) {
                return bounds.x();
            }
        }
        return component.defaultAnchor().baseX(screenWidth, width) + component.defaultOffsetX();
    }

    public static float defaultY(VanillaHudComponent component, int screenHeight, float height) {
        if (component == VanillaHudComponent.SCOREBOARD) {
            VanillaHudMeasurements.Bounds bounds = VanillaHudMeasurements.scoreboard();
            if (bounds.valid()) {
                return bounds.y();
            }
        }
        return component.defaultAnchor().baseY(screenHeight, height) + component.defaultOffsetY();
    }

    public static float targetX(HudElement element, VanillaHudComponent component, int screenWidth, float width) {
        if (component == VanillaHudComponent.SCOREBOARD && VanillaHudProxyElement.hasDefaultLayout(element, component)) {
            return defaultX(component, screenWidth, width);
        }
        return element.anchor().baseX(screenWidth, width) + element.offsetX();
    }

    public static float targetY(HudElement element, VanillaHudComponent component, int screenHeight, float height) {
        if (component == VanillaHudComponent.SCOREBOARD && VanillaHudProxyElement.hasDefaultLayout(element, component)) {
            return defaultY(component, screenHeight, height);
        }
        return element.anchor().baseY(screenHeight, height) + element.offsetY();
    }
}
