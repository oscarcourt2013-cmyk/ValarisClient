package dev.valarisclient.v26_2.hud;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.vanilla.VanillaHudComponent;
import dev.valarisclient.core.hud.vanilla.VanillaHudProxyElement;
import dev.valarisclient.core.hud.vanilla.VanillaHudTransformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Applies HUD-editor layout transforms before vanilla {@link net.minecraft.client.gui.Hud} layers draw. */
public final class VanillaHudTransforms {

    // Measured bounds can change between push and pop (scoreboard capture runs in between),
    // so pop must not recompute the push decision.
    private static final boolean[] PUSHED = new boolean[VanillaHudComponent.values().length];

    private VanillaHudTransforms() {
    }

    public static void push(GuiGraphicsExtractor extractor, VanillaHudComponent component) {
        HudElement element = ValarisClient.get().hud().get(component.id());
        if (!(element instanceof VanillaHudProxyElement) || !element.isVisible()) {
            return;
        }
        if (VanillaHudProxyElement.hasDefaultLayout(element, component)
                && Math.abs(element.scale() - 1f) < 0.01f
                && Math.abs(element.rotation()) < 0.01f) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        float scale = element.scale();
        float localWidth = VanillaHudTransformHelper.localWidth(component);
        float localHeight = VanillaHudTransformHelper.localHeight(component);
        float width = localWidth * scale;
        float height = localHeight * scale;

        // Vanilla draws at its natural, unscaled position; the target uses the scaled footprint.
        float defaultX = VanillaHudTransformHelper.defaultX(component, screenWidth, localWidth);
        float defaultY = VanillaHudTransformHelper.defaultY(component, screenHeight, localHeight);
        float targetX = VanillaHudTransformHelper.targetX(element, component, screenWidth, width);
        float targetY = VanillaHudTransformHelper.targetY(element, component, screenHeight, height);

        float rotation = element.rotation();
        if (Math.abs(targetX - defaultX) < 0.01f && Math.abs(targetY - defaultY) < 0.01f
                && Math.abs(scale - 1f) < 0.01f && Math.abs(rotation) < 0.01f) {
            return;
        }

        var pose = extractor.pose();
        pose.pushMatrix();
        pose.translate(targetX + width / 2f, targetY + height / 2f);
        if (rotation != 0f) {
            pose.rotate((float) Math.toRadians(rotation));
        }
        if (Math.abs(scale - 1f) > 0.01f) {
            pose.scale(scale, scale);
        }
        pose.translate(-(defaultX + localWidth / 2f), -(defaultY + localHeight / 2f));
        PUSHED[component.ordinal()] = true;
    }

    public static void pop(GuiGraphicsExtractor extractor, VanillaHudComponent component) {
        if (!PUSHED[component.ordinal()]) {
            return;
        }
        PUSHED[component.ordinal()] = false;
        extractor.pose().popMatrix();
    }
}
