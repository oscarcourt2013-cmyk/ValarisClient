package dev.valarisclient.v1_21_11.hud;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.vanilla.VanillaHudComponent;
import dev.valarisclient.v1_21_11.mixin.GuiLayerInvoker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws only the vanilla HUD layers that map to visible {@link VanillaHudComponent}
 * proxies — no world, blur, chat, crosshair or other {@link Gui#render} work.
 */
public final class VanillaHudLayerRenderer {

    private VanillaHudLayerRenderer() {
    }

    public static void renderVisibleLayers(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Gui gui = minecraft.gui;
        GuiLayerInvoker layers = (GuiLayerInvoker) gui;

        if (isVisible(VanillaHudComponent.BOSSBAR)) {
            layers.ValarisClient$renderBossOverlay(graphics, deltaTracker);
        }
        if (isVisible(VanillaHudComponent.STATUS_EFFECTS)) {
            layers.ValarisClient$renderEffects(graphics, deltaTracker);
        }
        if (isVisible(VanillaHudComponent.HOTBAR) || isVisible(VanillaHudComponent.EXPERIENCE)) {
            layers.ValarisClient$renderHotbarAndDecorations(graphics, deltaTracker);
        }
        if (isVisible(VanillaHudComponent.SCOREBOARD)) {
            layers.ValarisClient$renderScoreboardSidebar(graphics, deltaTracker);
        }
    }

    private static boolean isVisible(VanillaHudComponent component) {
        HudElement element = ValarisClient.get().hud().get(component.id());
        return element != null && element.isVisible();
    }
}
