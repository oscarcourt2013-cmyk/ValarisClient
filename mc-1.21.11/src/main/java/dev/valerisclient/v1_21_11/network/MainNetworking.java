package dev.valerisclient.v1_21_11.network;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.hook.PrimeHooks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

/** Registers {@code valerisclient:main} and wires handshake sends. */
public final class MainNetworking {

    private MainNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(MainPayload.TYPE, MainPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MainPayload.TYPE, MainPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(MainPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PrimeHooks.onServerApiPayload(payload.json())));

        var api = ValerisClient.get().serverApi();
        api.setOutboundSender(MainNetworking::sendJson);
        api.setChannelProbe(() -> ClientPlayNetworking.canSend(MainPayload.TYPE));
    }

    private static void sendJson(String json) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ClientPlayNetworking.canSend(MainPayload.TYPE)) {
            ValerisClient.get().serverApi().markChannelAvailable(false);
            return;
        }
        ValerisClient.get().serverApi().markChannelAvailable(true);
        ClientPlayNetworking.send(new MainPayload(json == null ? "{}" : json));
    }
}
