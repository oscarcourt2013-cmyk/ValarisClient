package dev.stellarclient.v1_21_11.network;

import dev.stellarclient.core.StellarClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** JSON body on the {@code StellarClient:main} plugin channel. */
public record MainPayload(String json) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(StellarClient.MOD_ID, "main");
    public static final Type<MainPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, MainPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    MainPayload::json,
                    MainPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
