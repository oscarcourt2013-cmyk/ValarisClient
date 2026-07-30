package dev.valerisclient.core.hud.vanilla;

import dev.valerisclient.core.hud.HudManager;

/** Registers vanilla HUD proxy elements for the editor. */
public final class VanillaHudElements {

    private VanillaHudElements() {
    }

    public static void registerAll(HudManager hud) {
        for (VanillaHudComponent component : VanillaHudComponent.values()) {
            hud.register(new VanillaHudProxyElement(component));
        }
    }
}
