package com.stellarclient.network;

import com.google.gson.JsonObject;
import com.stellarclient.StellarClientPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Pushes lightweight presence / stats to the Prime REST backend (optional).
 * Redis mode is stubbed for future multi-server fanout.
 */
public final class NetworkService {

    private final StellarClientPlugin plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private BukkitTask heartbeat;

    public NetworkService(StellarClientPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("network.enabled", false)) {
            return;
        }
        heartbeat = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::heartbeat, 20L * 60, 20L * 60);
    }

    public void shutdown() {
        if (heartbeat != null) {
            heartbeat.cancel();
        }
    }

    public void reportJoin(Player player) {
        postEvent("player_join", player.getUniqueId(), player.getName(), null);
    }

    public void reportQuit(Player player) {
        postEvent("player_quit", player.getUniqueId(), player.getName(), null);
    }

    public void reportPrime(Player player, String version) {
        postEvent("prime_detected", player.getUniqueId(), player.getName(), version);
    }

    private void heartbeat() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.detection().isVerified(player.getUniqueId())) {
                postEvent("heartbeat", player.getUniqueId(), player.getName(),
                        plugin.detection().getVersion(player.getUniqueId()).orElse(""));
            }
        }
    }

    private void postEvent(String type, UUID uuid, String name, String version) {
        if (!plugin.getConfig().getBoolean("network.enabled", false)) {
            return;
        }
        String mode = plugin.getConfig().getString("network.mode", "rest");
        if (!"rest".equalsIgnoreCase(mode)) {
            // Redis reserved — log once at fine level.
            return;
        }
        String base = plugin.getConfig().getString("network.rest-url", "");
        if (base == null || base.isBlank()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("t", type);
        body.addProperty("serverId", plugin.getConfig().getString("server-id", "unknown"));
        body.addProperty("serverName", plugin.getConfig().getString("server-name", "Server"));
        body.addProperty("uuid", uuid.toString());
        body.addProperty("username", name);
        if (version != null) {
            body.addProperty("clientVersion", version);
        }
        body.addProperty("onlinePlayers", Bukkit.getOnlinePlayers().size());
        body.addProperty("ts", System.currentTimeMillis());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String token = plugin.getConfig().getString("network.rest-token", "");
                HttpRequest.Builder b = HttpRequest.newBuilder()
                        .uri(URI.create(base.replaceAll("/$", "") + "/v1/network/event"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
                if (token != null && !token.isBlank()) {
                    b.header("Authorization", "Bearer " + token);
                }
                http.send(b.build(), HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                plugin.getLogger().fine("Network post failed: " + e.getMessage());
            }
        });
    }
}
