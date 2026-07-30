package dev.valarisclient.v26_2.render;

/** Accessor for Valaris cape/wings IDs stored on {@link net.minecraft.client.renderer.entity.state.AvatarRenderState}. */
public interface ValarisCosmeticRenderData {

    String ValarisClient$getCapeId();

    void ValarisClient$setCapeId(String capeId);

    String ValarisClient$getWingsId();

    void ValarisClient$setWingsId(String wingsId);
}
