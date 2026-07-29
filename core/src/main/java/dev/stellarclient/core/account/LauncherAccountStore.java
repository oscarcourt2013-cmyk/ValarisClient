package dev.stellarclient.core.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads/writes the StellarClient Launcher {@code accounts.json} so the in-game title menu
 * can switch Minecraft identities without relaunching.
 */
public final class LauncherAccountStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public record AccountEntry(
            String id,
            String type,
            String username,
            String uuid,
            Optional<String> msRefreshToken,
            Optional<String> msAuthProvider
    ) {
        public boolean microsoft() {
            return "microsoft".equalsIgnoreCase(type);
        }

        public boolean offline() {
            return !microsoft();
        }
    }

    public record SwitchPayload(
            String username,
            String uuid,
            String accessToken,
            boolean microsoft
    ) {
    }

    private LauncherAccountStore() {
    }

    public static Path accountsPath() {
        String appdata = System.getenv("APPDATA");
        if (appdata != null && !appdata.isBlank()) {
            return Path.of(appdata, "stellar-client-launcher", "accounts.json");
        }
        String home = System.getProperty("user.home", ".");
        return Path.of(home, ".stellar-client-launcher", "accounts.json");
    }

    public static List<AccountEntry> list() {
        JsonObject root = loadRoot();
        List<AccountEntry> out = new ArrayList<>();
        JsonArray accounts = root.has("accounts") && root.get("accounts").isJsonArray()
                ? root.getAsJsonArray("accounts")
                : new JsonArray();
        for (JsonElement el : accounts) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject a = el.getAsJsonObject();
            String id = str(a, "id");
            String username = str(a, "username");
            if (id.isBlank() || username.isBlank()) {
                continue;
            }
            String type = str(a, "type");
            if (type.isBlank()) {
                type = "offline";
            }
            String uuid = str(a, "uuid");
            if (uuid.isBlank()) {
                uuid = offlineUuid(username);
            }
            Optional<String> refresh = Optional.empty();
            if (a.has("msRefreshToken") && !a.get("msRefreshToken").isJsonNull()) {
                String t = a.get("msRefreshToken").getAsString();
                if (t != null && !t.isBlank()) {
                    refresh = Optional.of(t);
                }
            }
            Optional<String> provider = Optional.empty();
            if (a.has("msAuthProvider") && !a.get("msAuthProvider").isJsonNull()) {
                String p = a.get("msAuthProvider").getAsString();
                if (p != null && !p.isBlank()) {
                    provider = Optional.of(p);
                }
            }
            out.add(new AccountEntry(id, type, username, uuid, refresh, provider));
        }
        return out;
    }

    public static Optional<String> activeAccountId() {
        JsonObject root = loadRoot();
        if (root.has("activeAccountId") && !root.get("activeAccountId").isJsonNull()) {
            String id = root.get("activeAccountId").getAsString();
            if (id != null && !id.isBlank()) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    public static void setActive(String accountId) {
        JsonObject root = loadRoot();
        root.addProperty("activeAccountId", accountId);
        JsonArray accounts = root.has("accounts") ? root.getAsJsonArray("accounts") : new JsonArray();
        String username = "";
        String type = "offline";
        for (JsonElement el : accounts) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject a = el.getAsJsonObject();
            if (accountId.equals(str(a, "id"))) {
                username = str(a, "username");
                type = str(a, "type");
                a.addProperty("lastUsedAt", java.time.Instant.now().toString());
                break;
            }
        }
        if (root.has("primeAccount") && root.get("primeAccount").isJsonObject()) {
            JsonObject prime = root.getAsJsonObject("primeAccount");
            if (!username.isBlank()) {
                prime.addProperty("username", username);
            }
            prime.addProperty("tier", "microsoft".equalsIgnoreCase(type) ? "prime" : "free");
        }
        if (root.has("activeProfileId") && root.has("profiles") && root.get("profiles").isJsonArray()) {
            String profileId = root.get("activeProfileId").getAsString();
            for (JsonElement el : root.getAsJsonArray("profiles")) {
                if (!el.isJsonObject()) {
                    continue;
                }
                JsonObject p = el.getAsJsonObject();
                if (profileId.equals(str(p, "id"))) {
                    p.addProperty("minecraftAccountId", accountId);
                    break;
                }
            }
        }
        saveRoot(root);
    }

    public static Optional<AccountEntry> addOffline(String username) {
        String trimmed = username == null ? "" : username.trim();
        if (trimmed.length() < 3 || trimmed.length() > 16
                || !trimmed.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_')) {
            return Optional.empty();
        }
        JsonObject root = loadRoot();
        JsonArray accounts = root.has("accounts") && root.get("accounts").isJsonArray()
                ? root.getAsJsonArray("accounts")
                : new JsonArray();
        root.add("accounts", accounts);
        for (JsonElement el : accounts) {
            if (el.isJsonObject()
                    && trimmed.equalsIgnoreCase(str(el.getAsJsonObject(), "username"))) {
                return Optional.empty();
            }
        }
        String id = UUID.randomUUID().toString();
        String uuid = offlineUuid(trimmed);
        JsonObject account = new JsonObject();
        account.addProperty("id", id);
        account.addProperty("type", "offline");
        account.addProperty("username", trimmed);
        account.addProperty("uuid", uuid);
        account.addProperty("skinUrl", "https://mc-heads.net/avatar/" + uuid.replace("-", "") + "/64");
        account.addProperty("addedAt", java.time.Instant.now().toString());
        account.addProperty("lastUsedAt", java.time.Instant.now().toString());
        accounts.add(account);
        saveRoot(root);
        setActive(id);
        return Optional.of(new AccountEntry(id, "offline", trimmed, uuid, Optional.empty(), Optional.empty()));
    }

    public static SwitchPayload resolveForSwitch(AccountEntry account) throws IOException {
        if (account.offline()) {
            return new SwitchPayload(
                    account.username(),
                    account.uuid(),
                    "0",
                    false
            );
        }
        String refresh = account.msRefreshToken()
                .orElseThrow(() -> new IOException("Microsoft account needs a launcher re-login."));
        String preferred = account.msAuthProvider().orElse(null);
        MicrosoftTokenRefresh.Result tokens = MicrosoftTokenRefresh.refresh(refresh, preferred);
        persistRotatedRefresh(account.id(), tokens.refreshToken(), tokens.authProvider());
        return new SwitchPayload(
                tokens.username(),
                tokens.uuid(),
                tokens.accessToken(),
                true
        );
    }

    private static void persistRotatedRefresh(String accountId, String refreshToken, String authProvider) {
        JsonObject root = loadRoot();
        if (!root.has("accounts") || !root.get("accounts").isJsonArray()) {
            return;
        }
        for (JsonElement el : root.getAsJsonArray("accounts")) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject a = el.getAsJsonObject();
            if (accountId.equals(str(a, "id"))) {
                a.addProperty("msRefreshToken", refreshToken);
                if (authProvider != null && !authProvider.isBlank()) {
                    a.addProperty("msAuthProvider", authProvider);
                }
                break;
            }
        }
        saveRoot(root);
    }

    public static String offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8))
                .toString();
    }

    private static JsonObject loadRoot() {
        Path path = accountsPath();
        if (!Files.isRegularFile(path)) {
            JsonObject empty = new JsonObject();
            empty.addProperty("version", 1);
            empty.add("accounts", new JsonArray());
            return empty;
        }
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(raw);
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (Exception ignored) {
        }
        JsonObject empty = new JsonObject();
        empty.addProperty("version", 1);
        empty.add("accounts", new JsonArray());
        return empty;
    }

    private static void saveRoot(JsonObject root) {
        Path path = accountsPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static String str(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return "";
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return "";
        }
    }
}
