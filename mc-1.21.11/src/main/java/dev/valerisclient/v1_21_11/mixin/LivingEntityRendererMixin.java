package dev.valerisclient.v1_21_11.mixin;

import dev.valerisclient.core.hook.ValerisHooks;
import dev.valerisclient.core.state.HitColorState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "getModelTint", at = @At("RETURN"), cancellable = true)
    private void ValerisClient$customHitColor(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (HitColorState.active() && cir.getReturnValue() != 0) {
            cir.setReturnValue(HitColorState.argb());
        }
    }
}
