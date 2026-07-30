package dev.valarisclient.v1_21_11.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.valarisclient.core.hook.ValarisHooks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float ValarisClient$applyZoomFov(float fov) {
        float multiplier = ValarisHooks.fovMultiplier();
        return multiplier == 1.0f ? fov : fov * multiplier;
    }

    @ModifyReturnValue(method = "getDarkenWorldAmount", at = @At("RETURN"))
    private float ValarisClient$noWorldDarkening(float amount) {
        return ValarisHooks.fullbrightActive() ? 0.0F : amount;
    }
}
