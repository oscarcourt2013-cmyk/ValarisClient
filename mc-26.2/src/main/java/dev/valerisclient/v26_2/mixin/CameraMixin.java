package dev.valerisclient.v26_2.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.valerisclient.core.hook.PrimeHooks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In Minecraft 26.2, FOV lives on {@link Camera} ({@code calculateFov}/{@code getFov}),
 * not on {@code GameRenderer.getFov} (removed).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float ValerisClient$applyZoomFov(float fov) {
        float multiplier = PrimeHooks.fovMultiplier();
        return multiplier == 1.0f ? fov : fov * multiplier;
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void ValerisClient$cinematicCamera(net.minecraft.client.DeltaTracker deltaTracker, CallbackInfo ci) {
        if (PrimeHooks.cinematicCameraActive()) {
            setRotation(PrimeHooks.cinematicYaw(), PrimeHooks.cinematicPitch());
        }
    }
}
