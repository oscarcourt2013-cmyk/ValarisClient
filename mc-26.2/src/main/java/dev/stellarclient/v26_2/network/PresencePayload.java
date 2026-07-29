package dev.stellarclient.v26_2.network;

import dev.stellarclient.core.StellarClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Fabric payload announcing a StellarClient player + cosmetic loadout + skin hash. */
public record PresencePayload(UUID playerId, String capeId, String wingsId, String skinHash)
        implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(StellarClient.MOD_ID, "presence");
    public static final Type<PresencePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PresencePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    payload -> payload.playerId().getMostSignificantBits(),
                    ByteBufCodecs.VAR_LONG,
                    payload -> payload.playerId().getLeastSignificantBits(),
                    ByteBufCodecs.STRING_UTF8,
                    PresencePayload::capeId,
                    ByteBufCodecs.STRING_UTF8,
                    PresencePayload::wingsId,
                    ByteBufCodecs.STRING_UTF8,
                    PresencePayload::skinHash,
                    (msb, lsb, capeId, wingsId, skinHash) ->
                            new PresencePayload(new UUID(msb, lsb), capeId, wingsId, skinHash));

    public PresencePayload(UUID playerId) {
        this(playerId, "", "", "");
    }

    public PresencePayload(UUID playerId, String capeId, String wingsId) {
        this(playerId, capeId, wingsId, "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
