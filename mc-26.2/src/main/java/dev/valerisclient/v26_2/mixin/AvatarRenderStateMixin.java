package dev.valerisclient.v26_2.mixin;

import dev.valerisclient.v26_2.render.ValerisCosmeticRenderData;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements ValerisCosmeticRenderData {

    @Unique
    private String ValerisClient$capeId = "";

    @Unique
    private String ValerisClient$wingsId = "";

    @Override
    public String ValerisClient$getCapeId() {
        return ValerisClient$capeId;
    }

    @Override
    public void ValerisClient$setCapeId(String capeId) {
        this.ValerisClient$capeId = capeId != null ? capeId : "";
    }

    @Override
    public String ValerisClient$getWingsId() {
        return ValerisClient$wingsId;
    }

    @Override
    public void ValerisClient$setWingsId(String wingsId) {
        this.ValerisClient$wingsId = wingsId != null ? wingsId : "";
    }
}
