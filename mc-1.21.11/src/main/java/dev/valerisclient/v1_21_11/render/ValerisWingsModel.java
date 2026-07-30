package dev.valerisclient.v1_21_11.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;

/** Two-wing cosmetic model with a light flap animation. */
public final class ValerisWingsModel extends EntityModel<AvatarRenderState> {

    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public ValerisWingsModel(ModelPart root) {
        super(root);
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -8.0F, 0.0F, 14.0F, 16.0F, 1.0F),
                PartPose.offset(2.0F, 2.0F, 2.5F));
        root.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -8.0F, 0.0F, 14.0F, 16.0F, 1.0F),
                PartPose.offset(-2.0F, 2.0F, 2.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        float flap = Mth.sin(state.ageInTicks * 0.45F) * 0.55F;
        if (state.walkAnimationSpeed > 0.5F) {
            flap *= 1.35F;
        }
        leftWing.zRot = 0.45F + flap;
        rightWing.zRot = -(0.45F + flap);
        leftWing.yRot = 0.25F;
        rightWing.yRot = -0.25F;
        leftWing.xRot = 0.15F + state.capeLean * 0.002F;
        rightWing.xRot = 0.15F + state.capeLean * 0.002F;
    }
}
