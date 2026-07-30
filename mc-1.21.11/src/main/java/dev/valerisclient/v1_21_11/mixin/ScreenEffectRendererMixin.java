package dev.valerisclient.v1_21_11.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.valerisclient.core.hook.ValerisHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lowers the first-person fire overlay by applying an extra downward translate after
 * vanilla's own {@code PoseStack.translate} inside {@code renderFire}.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(
            method = "renderFire",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
                    shift = At.Shift.AFTER))
    private static void ValerisClient$lowFire(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            TextureAtlasSprite sprite,
            CallbackInfo ci) {
        if (ValerisHooks.lowFireActive()) {
            poseStack.translate(0.0F, -ValerisHooks.lowFireHeightOffset(), 0.0F);
        }
    }
}
