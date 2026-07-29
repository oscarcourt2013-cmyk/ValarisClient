package dev.stellarclient.v1_21_11.network;

import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.hook.PrimeHooks;
import dev.stellarclient.core.skin.CustomSkinService;
import dev.stellarclient.core.state.ClientBadgeState;
import dev.stellarclient.core.state.CosmeticsState;
import dev.stellarclient.core.state.CustomSkinState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Registers Prime presence + custom skin texture payloads for LAN / integrated servers. */
public final class PresenceNetworking {

    private static String lastSentSkinHash = "";
    /** Peers we already pushed our skin texture to this session (avoids 128KB spam). */
    private static final Set<UUID> skinPushedTo = ConcurrentHashMap.newKeySet();

    private PresenceNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(PresencePayload.TYPE, PresencePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PresencePayload.TYPE, PresencePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SkinTexturePayload.TYPE, SkinTexturePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SkinTexturePayload.TYPE, SkinTexturePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(PresencePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    PrimeHooks.onPresencePayload(
                            payload.playerId(), payload.capeId(), payload.wingsId(), payload.skinHash());
                    maybePushSkinToPeer(payload.playerId());
                }));

        ClientPlayNetworking.registerGlobalReceiver(SkinTexturePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.png() != null && payload.png().length <= CustomSkinService.MAX_BYTES) {
                        PrimeHooks.onSkinTexturePayload(payload.playerId(), payload.png());
                    }
                }));

        StellarClient.get().presence().setNetworkAnnouncer(PresenceNetworking::sendLocalPresence);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            lastSentSkinHash = "";
            skinPushedTo.clear();
            if (ClientBadgeState.active()) {
                client.execute(PresenceNetworking::sendLocalPresence);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastSentSkinHash = "";
            skinPushedTo.clear();
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerPlayNetworking.registerGlobalReceiver(PresencePayload.TYPE, (payload, context) -> {
                ServerPlayer sender = context.player();
                UUID senderId = sender.getUUID();
                PresencePayload forward = new PresencePayload(
                        senderId, payload.capeId(), payload.wingsId(), payload.skinHash());
                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    if (target == sender) {
                        continue;
                    }
                    ServerPlayNetworking.send(target, forward);
                }
            });
            ServerPlayNetworking.registerGlobalReceiver(SkinTexturePayload.TYPE, (payload, context) -> {
                ServerPlayer sender = context.player();
                UUID senderId = sender.getUUID();
                byte[] png = payload.png();
                if (png == null || png.length == 0 || png.length > CustomSkinService.MAX_BYTES) {
                    return;
                }
                SkinTexturePayload forward = new SkinTexturePayload(senderId, png);
                for (ServerPlayer target : server.getPlayerList().getPlayers()) {
                    if (target == sender) {
                        continue;
                    }
                    ServerPlayNetworking.send(target, forward);
                }
            });
        });
    }

    private static void sendLocalPresence() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ClientPlayNetworking.canSend(PresencePayload.TYPE)) {
            return;
        }
        String skinHash = CustomSkinState.localHash();
        boolean skinChanged = skinHash != null && !skinHash.equals(lastSentSkinHash);
        ClientPlayNetworking.send(new PresencePayload(
                client.player.getUUID(),
                CosmeticsState.localCapeId(),
                CosmeticsState.localWingsId(),
                skinHash == null ? "" : skinHash));
        if (skinChanged || lastSentSkinHash.isEmpty()) {
            // Broadcast to everyone on join / skin change (server relays to all peers).
            skinPushedTo.clear();
            sendLocalSkinTexture();
        }
    }

    private static void maybePushSkinToPeer(UUID peerId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || peerId == null || peerId.equals(client.player.getUUID())) {
            return;
        }
        if (!CustomSkinState.hasLocal()) {
            return;
        }
        if (!skinPushedTo.add(peerId)) {
            return;
        }
        sendLocalSkinTexture();
    }

    private static void sendLocalSkinTexture() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ClientPlayNetworking.canSend(SkinTexturePayload.TYPE)) {
            return;
        }
        byte[] png = CustomSkinState.localBytes();
        String hash = CustomSkinState.localHash();
        if (png == null || png.length == 0) {
            lastSentSkinHash = "";
            return;
        }
        ClientPlayNetworking.send(new SkinTexturePayload(client.player.getUUID(), png));
        lastSentSkinHash = hash == null ? "" : hash;
    }
}
