package dev.valerisclient.v26_2.render;

/** Accessor for Prime cape/wings IDs stored on {@link net.minecraft.client.renderer.entity.state.AvatarRenderState}. */
public interface ValerisCosmeticRenderData {

    String ValerisClient$getCapeId();

    void ValerisClient$setCapeId(String capeId);

    String ValerisClient$getWingsId();

    void ValerisClient$setWingsId(String wingsId);
}
