package dev.valerisclient.v26_2.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.valerisclient.core.hook.PrimeHooks;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(method = "submitFire", at = @At("HEAD"))
    private static void ValerisClient$lowFire(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            TextureAtlasSprite sprite,
            CallbackInfo ci) {
        if (PrimeHooks.lowFireActive()) {
            poseStack.translate(0.0F, -PrimeHooks.lowFireHeightOffset(), 0.0F);
        }
    }
}
