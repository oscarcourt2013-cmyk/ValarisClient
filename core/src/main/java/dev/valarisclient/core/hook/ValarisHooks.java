package dev.valarisclient.core.hook;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.event.AttackEntityEvent;
import dev.valarisclient.core.event.ChatMessageEvent;
import dev.valarisclient.core.event.PlayerDamageEvent;
import dev.valarisclient.core.event.PlayerDeathEvent;
import dev.valarisclient.core.state.CinematicCameraState;
import dev.valarisclient.core.state.ChatFilterState;
import dev.valarisclient.core.state.ChatOverlayState;
import dev.valarisclient.core.state.ZoomState;
import dev.valarisclient.core.state.ClientBadgeState;
import dev.valarisclient.core.stream.StreamRedactor;
import dev.valarisclient.core.stream.StreamerPrivacyState;

import java.util.UUID;

/**
 * Bridge from version-layer Fabric hooks and mixins into the common core.
 *
 * <p>Every method is null-safe: hooks may fire before bootstrap or after
 * shutdown.</p>
 */
public final class ValarisHooks {

    private ValarisHooks() {
    }

    public static void onChatMessage(String text, boolean outgoing) {
        if (!outgoing && ChatFilterState.shouldFilter(text)) {
            return;
        }
        ValarisClient client = tryGet();
        if (client != null) {
            client.events().post(new ChatMessageEvent(text, outgoing));
        }
    }

    /** Formats incoming chat for display; called by version-layer chat hooks. */
    public static String formatChatMessage(String text, boolean outgoing, long timestampMillis) {
        if (outgoing) {
            return text;
        }
        String formatted = ChatOverlayState.formatIncoming(text, timestampMillis);
        if (streamChatRedact()) {
            formatted = StreamRedactor.redact(formatted);
        }
        return formatted;
    }

    /** Called by debug overlay mixins to replace F3 with a stream-safe view. */
    public static boolean streamDebugShield() {
        return StreamerPrivacyState.debugShield();
    }

    /** Called by chat mixins to redact sensitive chat content. */
    public static boolean streamChatRedact() {
        return StreamerPrivacyState.chatRedact();
    }

    /** Called by entity renderer mixins to mask player nametags. */
    public static boolean streamNameMask() {
        return StreamerPrivacyState.nameMask();
    }

    /** Whether the local player's nametag should be masked. */
    public static boolean streamNameMaskSelf() {
        return StreamerPrivacyState.maskSelf();
    }

    public static void onAttackEntity(String targetName) {
        ValarisClient client = tryGet();
        if (client != null) {
            client.events().post(new AttackEntityEvent(targetName));
        }
    }

    public static void onPlayerDamage(float amount) {
        ValarisClient client = tryGet();
        if (client != null) {
            client.events().post(new PlayerDamageEvent(amount));
        }
    }

    public static void onPlayerDeath(double x, double y, double z) {
        ValarisClient client = tryGet();
        if (client != null) {
            client.events().post(new PlayerDeathEvent(x, y, z));
        }
    }

    /** Called by the GameRenderer mixin to apply zoom FOV. */
    public static float fovMultiplier() {
        return ZoomState.multiplier();
    }

    /** Called by the Camera mixin to apply cinematic smoothing. */
    public static boolean cinematicCameraActive() {
        return CinematicCameraState.active();
    }

    public static float cinematicYaw() {
        return CinematicCameraState.yaw();
    }

    public static float cinematicPitch() {
        return CinematicCameraState.pitch();
    }

    public static boolean hideVanillaCrosshair() {
        return dev.valarisclient.core.state.CrosshairState.hideVanillaCrosshair();
    }

    /** Called by lightmap mixins to force maximum brightness. */
    public static boolean fullbrightActive() {
        return dev.valarisclient.core.state.FullbrightState.active();
    }

    /** Called by level mixins to hide client-side precipitation. */
    public static boolean noRainActive() {
        return dev.valarisclient.core.state.NoRainState.active();
    }

    /** Called by level mixins to render the world as daytime. */
    public static boolean alwaysDayActive() {
        return dev.valarisclient.core.state.AlwaysDayState.active();
    }

    /** Called by screen effect mixins to lower the fire overlay. */
    public static boolean lowFireActive() {
        return dev.valarisclient.core.state.LowFireState.active();
    }

    public static float lowFireHeightOffset() {
        return dev.valarisclient.core.state.LowFireState.heightOffset();
    }

    /** Called by item-in-hand mixins for first-person tint. */
    public static boolean handShaderActive() {
        return dev.valarisclient.core.state.HandShaderState.active();
    }

    public static float handShaderRed() {
        return dev.valarisclient.core.state.HandShaderState.red();
    }

    public static float handShaderGreen() {
        return dev.valarisclient.core.state.HandShaderState.green();
    }

    public static float handShaderBlue() {
        return dev.valarisclient.core.state.HandShaderState.blue();
    }

    public static int handShaderOverlayArgb() {
        int base = dev.valarisclient.core.state.HandShaderState.argb();
        float intensity = dev.valarisclient.core.state.HandShaderState.intensity();
        int a = Math.clamp(Math.round(0x40 * intensity), 0, 0xFF);
        return (a << 24) | (base & 0x00FFFFFF);
    }

    /** Called by tab-list mixins for open animation. */
    public static boolean tabAnimationActive() {
        return dev.valarisclient.core.state.TabAnimationState.active();
    }

    public static int tabAnimationDurationMs() {
        return dev.valarisclient.core.state.TabAnimationState.durationMs();
    }

    public static float tabAnimationSlidePixels() {
        return dev.valarisclient.core.state.TabAnimationState.slidePixels();
    }

    /** Called by tab-list mixins to decorate Valaris player names. */
    public static boolean clientBadgeActive() {
        return ClientBadgeState.active();
    }

    public static int clientBadgeBackground() {
        return ClientBadgeState.background();
    }

    public static int clientBadgeAccent() {
        return ClientBadgeState.accent();
    }

    public static int clientBadgeForeground() {
        return ClientBadgeState.foreground();
    }

    /** Whether the given player was discovered as a ValarisClient user. */
    public static boolean isValarisPlayer(UUID uuid) {
        ValarisClient client = tryGet();
        return client != null && client.presence().isPrime(uuid);
    }

    /** Called by version-layer networking when a presence payload is received. */
    public static void onPresencePayload(UUID playerId) {
        onPresencePayload(playerId, "", "");
    }

    /** Presence + cosmetic loadout from another ValarisClient peer. */
    public static void onPresencePayload(UUID playerId, String capeId, String wingsId) {
        onPresencePayload(playerId, capeId, wingsId, "");
    }

    /** Presence + cosmetics + optional custom skin hash. */
    public static void onPresencePayload(UUID playerId, String capeId, String wingsId, String skinHash) {
        ValarisClient client = tryGet();
        if (client != null) {
            client.presence().markPrime(playerId, capeId, wingsId, skinHash);
        }
    }

    /** PNG body texture from another Valaris peer. */
    public static void onSkinTexturePayload(UUID playerId, byte[] png) {
        ValarisClient client = tryGet();
        if (client == null || playerId == null || png == null) {
            return;
        }
        if (!dev.valarisclient.core.skin.CustomSkinService.isValidPng(png)) {
            return;
        }
        client.presence().markPrime(playerId);
        dev.valarisclient.core.state.CustomSkinState.setPeer(playerId, png);
    }

    /** JSON body from the {@code valarisclient:main} custom payload channel. */
    public static void onServerApiPayload(String json) {
        ValarisClient client = tryGet();
        if (client != null) {
            client.serverApi().onPayload(json);
        }
    }

    /**
     * Returns {@code false} when an outgoing chat message should be blocked
     * (client-only {@code /valaris} / {@code /ai} / {@code /skin} commands).
     */
    public static boolean allowOutgoingChat(String message) {
        return !handleClientOnlyCommand(message);
    }

    /**
     * Fabric {@code ALLOW_COMMAND} — the payload is the command <em>without</em>
     * the leading {@code /}. Returns {@code false} to cancel sending to the server.
     */
    public static boolean allowOutgoingCommand(String command) {
        if (command == null || command.isBlank()) {
            return true;
        }
        String normalized = command.startsWith("/") ? command : "/" + command;
        return !handleClientOnlyCommand(normalized);
    }

    private static boolean handleClientOnlyCommand(String message) {
        ValarisClient client = tryGet();
        if (client == null || message == null) {
            return false;
        }
        if (client.serverApi().handleClientCommand(message)) {
            return true;
        }
        return client.customSkins().handleClientCommand(message);
    }

    private static ValarisClient tryGet() {
        try {
            return ValarisClient.get();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }
}
