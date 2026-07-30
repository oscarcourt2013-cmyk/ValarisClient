package dev.valarisclient.v1_21_11.mixin;

import dev.valarisclient.core.hud.vanilla.VanillaHudMeasurements;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens/closes scoreboard fill capture around vanilla sidebar rendering. */
@Mixin(Gui.class)
public abstract class ScoreboardSidebarBoundsMixin {

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"))
    private void ValarisClient$beginScoreboardCapture(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
        VanillaHudMeasurements.beginScoreboardCapture();
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void ValarisClient$endScoreboardCapture(CallbackInfo ci) {
        VanillaHudMeasurements.endScoreboardCapture();
    }
}
