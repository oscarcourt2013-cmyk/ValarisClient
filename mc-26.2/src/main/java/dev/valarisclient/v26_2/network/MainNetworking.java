package dev.valarisclient.v26_2.network;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.hook.ValarisHooks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

/** Registers {@code valarisclient:main} and wires handshake sends. */
public final class MainNetworking {

    private MainNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(MainPayload.TYPE, MainPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MainPayload.TYPE, MainPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(MainPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ValarisHooks.onServerApiPayload(payload.json())));

        var api = ValarisClient.get().serverApi();
        api.setOutboundSender(MainNetworking::sendJson);
        api.setChannelProbe(() -> ClientPlayNetworking.canSend(MainPayload.TYPE));
    }

    private static void sendJson(String json) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ClientPlayNetworking.canSend(MainPayload.TYPE)) {
            ValarisClient.get().serverApi().markChannelAvailable(false);
            return;
        }
        ValarisClient.get().serverApi().markChannelAvailable(true);
        ClientPlayNetworking.send(new MainPayload(json == null ? "{}" : json));
    }
}
