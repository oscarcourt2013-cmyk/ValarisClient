package dev.valarisclient.v26_2.mixin;

import dev.valarisclient.core.hook.ValarisHooks;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {

    @Shadow
    private float blockLightFlicker;

    @Shadow
    private boolean needsUpdate;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$stableFullbrightTick(CallbackInfo ci) {
        if (ValarisHooks.fullbrightActive()) {
            this.blockLightFlicker = 0.0F;
            this.needsUpdate = true;
            ci.cancel();
        }
    }

    @Inject(method = "extract", at = @At("TAIL"))
    private void ValarisClient$applyFullbright(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (!ValarisHooks.fullbrightActive()) {
            return;
        }
        renderState.brightness = 15.0F;
        renderState.darknessEffectScale = 0.0F;
        renderState.bossOverlayWorldDarkening = 0.0F;
        renderState.nightVisionEffectIntensity = 1.0F;
        renderState.blockFactor = 1.5F;
        renderState.needsUpdate = true;
    }
}
