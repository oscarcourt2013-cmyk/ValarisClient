package dev.stellarclient.v1_21_11;

import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.hook.PrimeHooks;
import dev.stellarclient.v1_21_11.render.GuiRenderContext;
import dev.stellarclient.v1_21_11.network.MainNetworking;
import dev.stellarclient.v1_21_11.network.PresenceNetworking;
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
public final class StellarClientEntrypoint implements ClientModInitializer {

    private VersionAdapter adapter;

    @Override
    public void onInitializeClient() {
        adapter = new VersionAdapter();
        StellarClient.bootstrap(adapter);
        PresenceNetworking.register();
        MainNetworking.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                client.getTextureManager().getTexture(
                        Identifier.fromNamespaceAndPath(StellarClient.MOD_ID, "textures/gui/logo.png")));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            trackHealth();
            StellarClient.get().tick();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            adapter.markSessionStart();
            StellarClient.get().onWorldJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            adapter.markSessionEnd();
            StellarClient.get().onWorldLeave();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> StellarClient.get().shutdown());

        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                PrimeHooks.onChatMessage(message.getString(), false));
        ClientSendMessageEvents.ALLOW_CHAT.register(PrimeHooks::allowOutgoingChat);
        ClientSendMessageEvents.ALLOW_COMMAND.register(PrimeHooks::allowOutgoingCommand);
        ClientSendMessageEvents.CHAT.register(message ->
                PrimeHooks.onChatMessage(message, true));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player != null && entity != null) {
                PrimeHooks.onAttackEntity(entity.getName().getString());
            }
            return InteractionResult.PASS;
        });

        GuiRenderContext renderContext = new GuiRenderContext();
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(StellarClient.MOD_ID, "hud"),
                (graphics, deltaTracker) -> {
                    if (dev.stellarclient.core.hud.editor.HudEditorState.isActive()) {
                        return;
                    }
                    renderContext.prepare(graphics);
                    var client = StellarClient.get();
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
            PrimeHooks.onPlayerDamage(lastHealth - health);
        }
        if (player.isDeadOrDying() && lastHealth > 0) {
            PrimeHooks.onPlayerDeath(player.getX(), player.getY(), player.getZ());
        }
        lastHealth = health;
    }
}
