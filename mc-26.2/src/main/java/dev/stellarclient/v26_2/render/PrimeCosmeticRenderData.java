package dev.stellarclient.v26_2.render;

/** Accessor for Prime cape/wings IDs stored on {@link net.minecraft.client.renderer.entity.state.AvatarRenderState}. */
public interface PrimeCosmeticRenderData {

    String StellarClient$getCapeId();

    void StellarClient$setCapeId(String capeId);

    String StellarClient$getWingsId();

    void StellarClient$setWingsId(String wingsId);
}
