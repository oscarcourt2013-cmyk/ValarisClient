package dev.stellarclient.v1_21_11.mixin;

import dev.stellarclient.core.hook.PrimeHooks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("RETURN"))
    private void StellarClient$cinematicCamera(CallbackInfo ci) {
        if (PrimeHooks.cinematicCameraActive()) {
            setRotation(PrimeHooks.cinematicYaw(), PrimeHooks.cinematicPitch());
        }
    }
}
