package dev.valarisclient.v26_2.mixin;

import dev.valarisclient.core.hud.vanilla.VanillaHudMeasurements;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens/closes scoreboard fill capture around vanilla sidebar rendering. */
@Mixin(Hud.class)
public abstract class ScoreboardSidebarBoundsMixin {

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"))
    private void ValarisClient$beginScoreboardCapture(GuiGraphicsExtractor extractor, Objective objective, CallbackInfo ci) {
        VanillaHudMeasurements.beginScoreboardCapture();
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void ValarisClient$endScoreboardCapture(CallbackInfo ci) {
        VanillaHudMeasurements.endScoreboardCapture();
    }
}
