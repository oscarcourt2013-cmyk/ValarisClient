package dev.valerisclient.v1_21_11.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.cosmetics.CosmeticTextures;
import dev.valerisclient.v1_21_11.render.ValerisCosmeticRenderData;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {

    @Shadow
    @Final
    private HumanoidModel<AvatarRenderState> model;

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void ValerisClient$renderValerisCape(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                             AvatarRenderState state, float a, float b, CallbackInfo ci) {
        if (!(state instanceof ValerisCosmeticRenderData data)) {
            return;
        }
        String path = CosmeticTextures.capePath(data.ValerisClient$getCapeId());
        if (path == null) {
            return;
        }
        if (state.isInvisible || state.chestEquipment.is(Items.ELYTRA)) {
            ci.cancel();
            return;
        }
        Identifier texture = Identifier.fromNamespaceAndPath(ValerisClient.MOD_ID, path);
        poseStack.pushPose();
        if (!state.chestEquipment.isEmpty()) {
            poseStack.translate(0.0F, -0.053125F, 0.06875F);
        }
        collector.submitModel(
                this.model,
                state,
                poseStack,
                RenderTypes.entitySolid(texture),
                light,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null);
        poseStack.popPose();
        ci.cancel();
    }
}
