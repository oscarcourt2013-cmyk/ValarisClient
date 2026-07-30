package dev.valerisclient.core.hud.vanilla;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.hud.HudElement;

/**
 * Placeholder HUD element for a vanilla Minecraft widget.
 * Vanilla still draws the real widget; this element exists for editor selection and layout.
 */
public final class VanillaHudProxyElement extends HudElement {

    private final VanillaHudComponent component;

    public VanillaHudProxyElement(VanillaHudComponent component) {
        super(component.id(), component.label(), component.defaultAnchor(),
                component.defaultOffsetX(), component.defaultOffsetY());
        this.component = component;
    }

    public VanillaHudComponent component() {
        return component;
    }

    @Override
    public int measureWidth(RenderContext ctx) {
        return VanillaHudTransformHelper.localWidth(component);
    }

    @Override
    public int measureHeight(RenderContext ctx) {
        return VanillaHudTransformHelper.localHeight(component);
    }

    public static boolean hasDefaultLayout(HudElement element, VanillaHudComponent component) {
        return element.anchor() == component.defaultAnchor()
                && Math.abs(element.offsetX() - component.defaultOffsetX()) < 0.01f
                && Math.abs(element.offsetY() - component.defaultOffsetY()) < 0.01f;
    }

    @Override
    public void render(RenderContext ctx, long nowMillis) {
        // Vanilla Minecraft renders this layer; proxy is editor-only.
    }
}
