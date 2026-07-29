package dev.stellarclient.core.serverapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.social.SocialClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Session cache for Prime account data pushed by compatible servers,
 * plus a live view of social friends when available.
 */
public final class PrimeAccountManager {

    public record FriendView(String uuid, String username, String status, String serverAddress) {
    }

    private final SocialClient socialClient;

    private volatile boolean logged;
    private volatile int level;
    private volatile int xp;
    private volatile List<String> availableRewards = List.of();
    private volatile List<FriendView> syncedFriends = List.of();

    public PrimeAccountManager(SocialClient socialClient) {
        this.socialClient = socialClient;
    }

    public int getLevel() {
        return level;
    }

    public int getXP() {
        return xp;
    }

    public boolean isLogged() {
        return logged;
    }

    public List<String> availableRewards() {
        return availableRewards;
    }

    /**
     * Friends currently online / in-game. Prefers live social cache;
     * falls back to last {@code ACCOUNT_SYNC} payload.
     */
    public List<FriendView> getFriends() {
        Map<String, SocialClient.Friend> live = socialClient.friends();
        if (!live.isEmpty()) {
            List<FriendView> out = new ArrayList<>();
            for (SocialClient.Friend f : live.values()) {
                if (f.pending()) {
                    continue;
                }
                String status = f.status() == null ? "offline" : f.status().toLowerCase(Locale.ROOT);
                if (!"online".equals(status) && !"in-game".equals(status) && !"away".equals(status)) {
                    continue;
                }
                out.add(new FriendView(f.uuid(), f.username(), status, f.serverAddress()));
            }
            return Collections.unmodifiableList(out);
        }
        return syncedFriends;
    }

    public void applyAccountSync(JsonObject payload) {
        logged = ServerApiProtocol.bool(payload, "logged", true);
        level = ServerApiProtocol.integer(payload, "level", level);
        xp = ServerApiProtocol.integer(payload, "xp", xp);
        if (payload.has("rewards") && payload.get("rewards").isJsonArray()) {
            List<String> rewards = new ArrayList<>();
            JsonArray arr = payload.getAsJsonArray("rewards");
            for (JsonElement el : arr) {
                if (el != null && el.isJsonPrimitive()) {
                    rewards.add(el.getAsString());
                }
            }
            availableRewards = List.copyOf(rewards);
        }
        if (payload.has("friends") && payload.get("friends").isJsonArray()) {
            List<FriendView> friends = new ArrayList<>();
            for (JsonElement el : payload.getAsJsonArray("friends")) {
                if (el == null || !el.isJsonObject()) {
                    continue;
                }
                JsonObject f = el.getAsJsonObject();
                friends.add(new FriendView(
                        ServerApiProtocol.str(f, "uuid", ""),
                        ServerApiProtocol.str(f, "username", "Player"),
                        ServerApiProtocol.str(f, "status", "online"),
                        ServerApiProtocol.str(f, "serverAddress", null)));
            }
            syncedFriends = List.copyOf(friends);
        }
    }

    public void addXp(int amount) {
        if (amount <= 0) {
            return;
        }
        xp += amount;
        while (xp >= xpForNextLevel(level) && level < 999) {
            xp -= xpForNextLevel(level);
            level++;
        }
    }

    public void resetSession() {
        logged = false;
        // Keep level/xp locally for the session UI until next sync; clear rewards.
        availableRewards = List.of();
        syncedFriends = List.of();
    }

    private static int xpForNextLevel(int currentLevel) {
        return 100 + Math.max(0, currentLevel) * 25;
    }
}
