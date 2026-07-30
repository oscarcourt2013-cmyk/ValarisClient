package dev.valerisclient.v26_2;

import dev.valerisclient.core.ValerisClient;
import dev.valerisclient.core.hook.ValerisHooks;
import dev.valerisclient.v26_2.render.GuiRenderContext;
import dev.valerisclient.v26_2.network.MainNetworking;
import dev.valerisclient.v26_2.network.PresenceNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

/**
 * Fabric client entrypoint for the Minecraft 26.2 layer.
 */
public final class ValerisClientEntrypoint implements ClientModInitializer {

    private VersionAdapter adapter;

    @Override
    public void onInitializeClient() {
        adapter = new VersionAdapter();
        ValerisClient.bootstrap(adapter);
        PresenceNetworking.register();
        MainNetworking.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                client.getTextureManager().getTexture(
                        Identifier.fromNamespaceAndPath(ValerisClient.MOD_ID, "textures/gui/logo.png")));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            trackHealth();
            ValerisClient.get().tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            adapter.markSessionStart();
            ValerisClient.get().onWorldJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            adapter.markSessionEnd();
            ValerisClient.get().onWorldLeave();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ValerisClient.get().shutdown());

        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                ValerisHooks.onChatMessage(message.getString(), false));
        ClientSendMessageEvents.ALLOW_CHAT.register(ValerisHooks::allowOutgoingChat);
        ClientSendMessageEvents.ALLOW_COMMAND.register(ValerisHooks::allowOutgoingCommand);
        ClientSendMessageEvents.CHAT.register(message ->
                ValerisHooks.onChatMessage(message, true));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player != null && entity != null) {
                ValerisHooks.onAttackEntity(entity.getName().getString());
            }
            return InteractionResult.PASS;
        });

        GuiRenderContext renderContext = new GuiRenderContext();
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(ValerisClient.MOD_ID, "hud"),
                (extractor, deltaTracker) -> {
                    if (dev.valerisclient.core.hud.editor.HudEditorState.isActive()) {
                        return;
                    }
                    renderContext.prepare(extractor);
                    var client = ValerisClient.get();
                    client.hud().render(renderContext);
                    if (client.loadingOverlay().visible()) {
                        client.loadingOverlay().render(renderContext, client.themes().active());
                    }
                });
    }

    private float lastHealth = -1;

    private void trackHealth() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) {
            lastHealth = -1;
            return;
        }
        float health = player.getHealth();
        if (lastHealth >= 0 && health < lastHealth) {
            ValerisHooks.onPlayerDamage(lastHealth - health);
        }
        if (player.isDeadOrDying() && lastHealth > 0) {
            ValerisHooks.onPlayerDeath(player.getX(), player.getY(), player.getZ());
        }
        lastHealth = health;
    }
}
