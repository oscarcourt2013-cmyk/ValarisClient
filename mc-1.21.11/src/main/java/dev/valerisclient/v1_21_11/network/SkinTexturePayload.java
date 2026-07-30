package dev.valerisclient.v1_21_11.network;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.skin.CustomSkinService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Relays custom skin PNG bytes between ValerisClient peers. */
public record SkinTexturePayload(UUID playerId, byte[] png) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(ValerisClient.MOD_ID, "skin_texture");
    public static final Type<SkinTexturePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SkinTexturePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    payload -> payload.playerId().getMostSignificantBits(),
                    ByteBufCodecs.VAR_LONG,
                    payload -> payload.playerId().getLeastSignificantBits(),
                    ByteBufCodecs.BYTE_ARRAY,
                    SkinTexturePayload::png,
                    (msb, lsb, png) -> new SkinTexturePayload(new UUID(msb, lsb), clamp(png)));

    private static byte[] clamp(byte[] png) {
        if (png == null) {
            return new byte[0];
        }
        if (png.length > CustomSkinService.MAX_BYTES) {
            byte[] trimmed = new byte[CustomSkinService.MAX_BYTES];
            System.arraycopy(png, 0, trimmed, 0, CustomSkinService.MAX_BYTES);
            return trimmed;
        }
        return png;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
