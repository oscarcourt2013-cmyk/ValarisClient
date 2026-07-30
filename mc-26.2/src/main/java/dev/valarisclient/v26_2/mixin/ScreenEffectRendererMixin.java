package dev.valarisclient.v26_2.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.valarisclient.core.hook.ValarisHooks;
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
    private static void ValarisClient$lowFire(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            TextureAtlasSprite sprite,
            CallbackInfo ci) {
        if (ValarisHooks.lowFireActive()) {
            poseStack.translate(0.0F, -ValarisHooks.lowFireHeightOffset(), 0.0F);
        }
    }
}
