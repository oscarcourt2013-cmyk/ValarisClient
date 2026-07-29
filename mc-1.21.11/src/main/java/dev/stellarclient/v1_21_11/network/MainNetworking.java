package dev.stellarclient.v1_21_11.network;

import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.hook.PrimeHooks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

/** Registers {@code stellarclient:main} and wires handshake sends. */
public final class MainNetworking {

    private MainNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(MainPayload.TYPE, MainPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MainPayload.TYPE, MainPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(MainPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PrimeHooks.onServerApiPayload(payload.json())));

        var api = StellarClient.get().serverApi();
        api.setOutboundSender(MainNetworking::sendJson);
        api.setChannelProbe(() -> ClientPlayNetworking.canSend(MainPayload.TYPE));
    }

    private static void sendJson(String json) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ClientPlayNetworking.canSend(MainPayload.TYPE)) {
            StellarClient.get().serverApi().markChannelAvailable(false);
            return;
        }
        StellarClient.get().serverApi().markChannelAvailable(true);
        ClientPlayNetworking.send(new MainPayload(json == null ? "{}" : json));
    }
}
