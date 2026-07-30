package dev.valarisclient.v26_2.mixin;

import dev.valarisclient.v26_2.render.ValarisCosmeticRenderData;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements ValarisCosmeticRenderData {

    @Unique
    private String ValarisClient$capeId = "";

    @Unique
    private String ValarisClient$wingsId = "";

    @Override
    public String ValarisClient$getCapeId() {
        return ValarisClient$capeId;
    }

    @Override
    public void ValarisClient$setCapeId(String capeId) {
        this.ValarisClient$capeId = capeId != null ? capeId : "";
    }

    @Override
    public String ValarisClient$getWingsId() {
        return ValarisClient$wingsId;
    }

    @Override
    public void ValarisClient$setWingsId(String wingsId) {
        this.ValarisClient$wingsId = wingsId != null ? wingsId : "";
    }
}
