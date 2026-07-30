package dev.valarisclient.v1_21_11.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.cosmetics.CosmeticTextures;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

/** Renders animated Valaris cosmetic wings behind the player. */
public final class ValarisWingsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private final ValarisWingsModel model;

    public ValarisWingsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
        this.model = new ValarisWingsModel(ValarisWingsModel.createBodyLayer().bakeRoot());
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float limbSwing, float limbSwingAmount) {
        if (!(state instanceof ValarisCosmeticRenderData data)) {
            return;
        }
        if (state.isInvisible) {
            return;
        }
        String path = CosmeticTextures.wingsPath(data.ValarisClient$getWingsId());
        if (path == null) {
            return;
        }
        if (state.chestEquipment.is(Items.ELYTRA)) {
            return;
        }
        Identifier texture = Identifier.fromNamespaceAndPath(ValarisClient.MOD_ID, path);
        model.setupAnim(state);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.12F);
        collector.submitModel(
                model,
                state,
                poseStack,
                RenderTypes.entityTranslucent(texture),
                light,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null);
        poseStack.popPose();
    }
}
