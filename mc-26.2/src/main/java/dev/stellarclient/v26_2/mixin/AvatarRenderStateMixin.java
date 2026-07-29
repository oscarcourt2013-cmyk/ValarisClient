package dev.stellarclient.v26_2.mixin;

import dev.stellarclient.v26_2.render.PrimeCosmeticRenderData;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements PrimeCosmeticRenderData {

    @Unique
    private String StellarClient$capeId = "";

    @Unique
    private String StellarClient$wingsId = "";

    @Override
    public String StellarClient$getCapeId() {
        return StellarClient$capeId;
    }

    @Override
    public void StellarClient$setCapeId(String capeId) {
        this.StellarClient$capeId = capeId != null ? capeId : "";
    }

    @Override
    public String StellarClient$getWingsId() {
        return StellarClient$wingsId;
    }

    @Override
    public void StellarClient$setWingsId(String wingsId) {
        this.StellarClient$wingsId = wingsId != null ? wingsId : "";
    }
}
