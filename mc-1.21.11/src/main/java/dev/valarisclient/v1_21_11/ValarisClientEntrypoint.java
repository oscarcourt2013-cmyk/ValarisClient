package dev.valarisclient.v1_21_11;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.hook.ValarisHooks;
import dev.valarisclient.v1_21_11.render.GuiRenderContext;
import dev.valarisclient.v1_21_11.network.MainNetworking;
import dev.valarisclient.v1_21_11.network.PresenceNetworking;
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
 * Fabric client entrypoint for the Minecraft 1.21.11 layer.
 */
public final class ValarisClientEntrypoint implements ClientModInitializer {

    private VersionAdapter adapter;

    @Override
    public void onInitializeClient() {
        adapter = new VersionAdapter();
        ValarisClient.bootstrap(adapter);
        PresenceNetworking.register();
        MainNetworking.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                client.getTextureManager().getTexture(
                        Identifier.fromNamespaceAndPath(ValarisClient.MOD_ID, "textures/gui/logo.png")));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            trackHealth();
            ValarisClient.get().tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            adapter.markSessionStart();
            ValarisClient.get().onWorldJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            adapter.markSessionEnd();
            ValarisClient.get().onWorldLeave();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ValarisClient.get().shutdown());

        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                ValarisHooks.onChatMessage(message.getString(), false));
        ClientSendMessageEvents.ALLOW_CHAT.register(ValarisHooks::allowOutgoingChat);
        ClientSendMessageEvents.ALLOW_COMMAND.register(ValarisHooks::allowOutgoingCommand);
        ClientSendMessageEvents.CHAT.register(message ->
                ValarisHooks.onChatMessage(message, true));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player != null && entity != null) {
                ValarisHooks.onAttackEntity(entity.getName().getString());
            }
            return InteractionResult.PASS;
        });

        GuiRenderContext renderContext = new GuiRenderContext();
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(ValarisClient.MOD_ID, "hud"),
                (graphics, deltaTracker) -> {
                    if (dev.valarisclient.core.hud.editor.HudEditorState.isActive()) {
                        return;
                    }
                    renderContext.prepare(graphics);
                    var client = ValarisClient.get();
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
            ValarisHooks.onPlayerDamage(lastHealth - health);
        }
        if (player.isDeadOrDying() && lastHealth > 0) {
            ValarisHooks.onPlayerDeath(player.getX(), player.getY(), player.getZ());
        }
        lastHealth = health;
    }
}
