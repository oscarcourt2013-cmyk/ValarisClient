package dev.valarisclient.v1_21_11.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.valarisclient.core.hook.ValarisHooks;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @Shadow
    @Final
    private GpuTexture texture;

    @Shadow
    private float blockLightRedFlicker;

    @Shadow
    private boolean updateLightTexture;

    @Inject(method = "updateLightTexture", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$fullbrightLightmap(float partialTick, CallbackInfo ci) {
        if (!ValarisHooks.fullbrightActive()) {
            return;
        }
        this.blockLightRedFlicker = 0.0F;
        this.updateLightTexture = false;
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.texture, -1);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ValarisClient$stableFullbrightTick(CallbackInfo ci) {
        if (ValarisHooks.fullbrightActive()) {
            this.blockLightRedFlicker = 0.0F;
            this.updateLightTexture = true;
            ci.cancel();
        }
    }

    @Inject(method = "calculateDarknessScale", at = @At("RETURN"), cancellable = true)
    private void ValarisClient$noDarknessPulse(CallbackInfoReturnable<Float> cir) {
        if (ValarisHooks.fullbrightActive()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getBrightness(FI)F", at = @At("RETURN"), cancellable = true)
    private static void ValarisClient$fullbrightLevel(CallbackInfoReturnable<Float> cir) {
        if (ValarisHooks.fullbrightActive()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(
            method = "getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void ValarisClient$fullbrightDimension(CallbackInfoReturnable<Float> cir) {
        if (ValarisHooks.fullbrightActive()) {
            cir.setReturnValue(1.0F);
        }
    }
}
