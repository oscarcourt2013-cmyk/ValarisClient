package com.stellarclient.friends;

import com.stellarclient.StellarClientPlugin;
import com.stellarclient.notifications.NotificationService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Soft integration with the Prime social backend.
 * When REST is configured, fetches friend UUIDs; otherwise no-ops safely.
 */
public final class FriendsService {

    private final StellarClientPlugin plugin;
    private final NotificationService notifications;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public FriendsService(StellarClientPlugin plugin, NotificationService notifications) {
        this.plugin = plugin;
        this.notifications = notifications;
    }

    public void onPlayerJoin(Player joining) {
        if (!plugin.getConfig().getBoolean("friends.notify-join", true)) {
            return;
        }
        if (!plugin.getConfig().getBoolean("network.enabled", false)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<UUID> friendIds = fetchFriendIds(joining.getUniqueId());
            if (friendIds.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.getUniqueId().equals(joining.getUniqueId())) {
                        continue;
                    }
                    if (friendIds.contains(online.getUniqueId())
                            && plugin.detection().isVerified(online.getUniqueId())) {
                        notifications.friendJoined(online, joining.getName());
                    }
                }
            });
        });
    }

    private Set<UUID> fetchFriendIds(UUID uuid) {
        Set<UUID> out = new HashSet<>();
        String base = plugin.getConfig().getString("network.rest-url", "");
        String token = plugin.getConfig().getString("network.rest-token", "");
        if (base == null || base.isBlank() || token == null || token.isBlank()) {
            return out;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base.replaceAll("/$", "") + "/v1/friends"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                // Minimal parse: look for "uuid":"..."
                String body = res.body();
                int idx = 0;
                while ((idx = body.indexOf("\"uuid\"", idx)) >= 0) {
                    int start = body.indexOf('"', idx + 6);
                    if (start < 0) {
                        break;
                    }
                    start++;
                    int end = body.indexOf('"', start);
                    if (end < 0) {
                        break;
                    }
                    try {
                        out.add(UUID.fromString(body.substring(start, end)));
                    } catch (Exception ignored) {
                    }
                    idx = end + 1;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Friends fetch failed: " + e.getMessage());
        }
        return out;
    }
}
