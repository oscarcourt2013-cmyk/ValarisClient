package dev.valarisclient.v1_21_11.mixin;

import dev.valarisclient.core.hook.ValarisHooks;
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
    private void ValarisClient$cinematicCamera(CallbackInfo ci) {
        if (ValarisHooks.cinematicCameraActive()) {
            setRotation(ValarisHooks.cinematicYaw(), ValarisHooks.cinematicPitch());
        }
    }
}
