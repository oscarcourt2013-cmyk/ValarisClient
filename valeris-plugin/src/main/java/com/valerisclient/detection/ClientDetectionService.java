package com.valerisclient.detection;

import com.google.gson.JsonObject;
import com.valerisclient.ValerisClientPlugin;
import com.valerisclient.database.Database;
import com.valerisclient.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects ValerisClient via {@code valerisclient:main} handshake.
 * Rejects spoofed names / protocol mismatches to reduce false positives.
 */
public final class ClientDetectionService implements PluginMessageListener {

    /** Must stay lowercase and match ServerApiProtocol.CHANNEL on the mod side. */
    public static final String CHANNEL = "valerisclient:main";

    private final ValerisClientPlugin plugin;
    private final Database database;
    private final Set<UUID> verified = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> versions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> verifiedAt = new ConcurrentHashMap<>();

    public ClientDetectionService(ValerisClientPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    public void register() {
        var messenger = Bukkit.getMessenger();
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void unregister() {
        var messenger = Bukkit.getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    public boolean isVerified(UUID uuid) {
        return uuid != null && verified.contains(uuid);
    }

    public Optional<String> getVersion(UUID uuid) {
        return Optional.ofNullable(versions.get(uuid));
    }

    public void clear(UUID uuid) {
        verified.remove(uuid);
        versions.remove(uuid);
        verifiedAt.remove(uuid);
    }

    public void sendJson(Player player, JsonObject payload) {
        if (player == null || !player.isOnline()) {
            return;
        }
        player.sendPluginMessage(plugin, CHANNEL, Text.encodePayload(payload.toString()));
    }

    public void sendAccountSync(Player player) {
        database.findProfile(player.getUniqueId()).ifPresent(profile -> {
            JsonObject sync = new JsonObject();
            sync.addProperty("t", "ACCOUNT_SYNC");
            sync.addProperty("logged", true);
            sync.addProperty("level", profile.level());
            sync.addProperty("xp", profile.xp());
            sendJson(player, sync);
        });
    }

    public void sendXpPacket(Player player, int amount, String kind) {
        JsonObject xp = new JsonObject();
        xp.addProperty("t", "XP");
        xp.addProperty("amount", amount);
        xp.addProperty("kind", kind == null ? "SERVER_EVENT" : kind);
        sendJson(player, xp);
    }

    public void sendNotify(Player player, String kind, String title, String message, boolean chat) {
        JsonObject n = new JsonObject();
        n.addProperty("t", "NOTIFY");
        n.addProperty("kind", kind);
        n.addProperty("title", title);
        n.addProperty("message", message);
        n.addProperty("chat", chat);
        sendJson(player, n);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        String json = Text.decodePayload(message);
        JsonObject obj = Text.parseJson(json);
        String type = Text.str(obj, "t", "");
        if ("HANDSHAKE".equals(type)) {
            handleHandshake(player, obj);
        } else if ("PROFILE".equals(type)) {
            // Client answered PROFILE_REQUEST — optional telemetry.
            plugin.getLogger().fine("PROFILE from " + player.getName() + ": " + json);
        }
    }

    private void handleHandshake(Player player, JsonObject obj) {
        String client = Text.str(obj, "client", "");
        String version = Text.str(obj, "version", "");
        int protocol = Text.integer(obj, "protocol", -1);
        UUID claimed = Text.parseUuid(Text.str(obj, "account", ""));

        int min = plugin.getConfig().getInt("protocol.min", 1);
        int max = plugin.getConfig().getInt("protocol.max", 1);

        boolean validClient = "ValerisClient".equalsIgnoreCase(client.trim());
        boolean validProtocol = protocol >= min && protocol <= max;
        boolean validUuid = claimed != null && claimed.equals(player.getUniqueId());

        if (!validClient || !validProtocol || !validUuid) {
            JsonObject reject = new JsonObject();
            reject.addProperty("t", "SERVER_REJECTED");
            reject.addProperty("message", plugin.getConfig().getString(
                    "protocol.reject-message", "Unsupported ValerisClient protocol"));
            sendJson(player, reject);
            plugin.getLogger().warning("Rejected Valeris handshake from " + player.getName()
                    + " (client=" + client + ", protocol=" + protocol + ", uuidMatch=" + validUuid + ")");
            return;
        }

        boolean first = database.findProfile(player.getUniqueId())
                .map(p -> !p.ValerisClient())
                .orElse(true);

        long now = System.currentTimeMillis();
        database.markValerisClient(player.getUniqueId(), player.getName(), version, now);
        verified.add(player.getUniqueId());
        versions.put(player.getUniqueId(), version);
        verifiedAt.put(player.getUniqueId(), now);

        if (plugin.getConfig().getBoolean("verified.grant-permission", true)) {
            player.addAttachment(plugin, "ValerisClient.verified", true);
        }

        JsonObject accept = new JsonObject();
        accept.addProperty("t", "SERVER_ACCEPTED");
        accept.addProperty("message", plugin.getConfig().getString(
                "protocol.accept-message", "Welcome — ValerisClient verified"));
        sendJson(player, accept);

        sendAccountSync(player);

        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.getPluginManager().callEvent(new ValerisClientDetectedEvent(player, version, first)));
    }
}
