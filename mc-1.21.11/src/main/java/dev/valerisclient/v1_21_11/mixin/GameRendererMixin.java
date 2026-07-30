package dev.valerisclient.v1_21_11.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.valerisclient.core.hook.PrimeHooks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float ValerisClient$applyZoomFov(float fov) {
        float multiplier = PrimeHooks.fovMultiplier();
        return multiplier == 1.0f ? fov : fov * multiplier;
    }

    @ModifyReturnValue(method = "getDarkenWorldAmount", at = @At("RETURN"))
    private float ValerisClient$noWorldDarkening(float amount) {
        return PrimeHooks.fullbrightActive() ? 0.0F : amount;
    }
}
