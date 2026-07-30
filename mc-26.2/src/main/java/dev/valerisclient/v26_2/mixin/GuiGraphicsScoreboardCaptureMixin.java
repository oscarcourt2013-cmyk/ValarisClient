package dev.valerisclient.v26_2.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.valerisclient.core.hud.vanilla.VanillaHudMeasurements;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records fill rects while the scoreboard sidebar is rendering. */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsScoreboardCaptureMixin {

    @Inject(method = "fill(IIIII)V", at = @At("HEAD"))
    private void ValerisClient$captureLegacyFill(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        VanillaHudMeasurements.recordScoreboardFill(x1, y1, x2, y2);
    }

    @Inject(method = "fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;IIIII)V", at = @At("HEAD"))
    private void ValerisClient$capturePipelineFill(
            RenderPipeline pipeline, int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        VanillaHudMeasurements.recordScoreboardFill(x1, y1, x2, y2);
    }
}
