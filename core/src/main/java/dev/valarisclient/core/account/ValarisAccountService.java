package dev.valarisclient.core.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.valarisclient.core.config.ConfigBinding;

import java.util.UUID;

/** Valaris Account auth stub — ready for real OAuth/API. */
public final class ValarisAccountService implements ConfigBinding {

    public enum Tier {
        FREE,
        /** Paid / Microsoft-linked Valaris tier (launcher {@code valaris}). */
        PREMIUM,
        /** Highest grade (launcher {@code valaris_plus}). */
        VALARIS_PLUS
    }

    private String username = "";
    private String uuid = "";
    private String token = "";
    private Tier tier = Tier.FREE;
    private boolean loggedIn;
    private long loginEpochMillis;

    public boolean loggedIn() {
        return loggedIn;
    }

    public String username() {
        return username;
    }

    public String uuid() {
        return uuid;
    }

    public String token() {
        return token;
    }

    public Tier tier() {
        return tier;
    }

    public long loginEpochMillis() {
        return loginEpochMillis;
    }

    public boolean login(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        this.username = name.trim();
        this.uuid = UUID.nameUUIDFromBytes(("Valaris:" + username).getBytes()).toString();
        this.token = "valaris-" + Integer.toHexString(name.hashCode());
        this.tier = Tier.PREMIUM;
        this.loggedIn = true;
        this.loginEpochMillis = System.currentTimeMillis();
        return true;
    }

    public boolean refreshSession() {
        if (!loggedIn || username.isBlank()) {
            return false;
        }
        token = "valaris-" + Integer.toHexString((username + System.currentTimeMillis()).hashCode());
        loginEpochMillis = System.currentTimeMillis();
        return true;
    }

    public void logout() {
        loggedIn = false;
        token = "";
        tier = Tier.FREE;
    }

    @Override
    public String configKey() {
        return "account";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("uuid", uuid);
        json.addProperty("tier", tier.name());
        json.addProperty("loggedIn", loggedIn);
        json.addProperty("loginEpoch", loginEpochMillis);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("username")) {
            username = json.get("username").getAsString();
        }
        if (json.has("uuid")) {
            uuid = json.get("uuid").getAsString();
        }
        if (json.has("tier")) {
            tier = parseTier(json.get("tier").getAsString());
        }
        if (json.has("loginEpoch")) {
            loginEpochMillis = json.get("loginEpoch").getAsLong();
        }
        if (json.has("loggedIn") && json.get("loggedIn").getAsBoolean() && !username.isBlank()) {
            login(username);
        }
    }

    /** Accepts enum names plus launcher ids {@code free}/{@code valaris}/{@code valaris_plus}. */
    static Tier parseTier(String raw) {
        if (raw == null || raw.isBlank()) {
            return Tier.FREE;
        }
        String key = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (key) {
            case "premium", "valaris" -> Tier.PREMIUM;
            case "valaris_plus", "valarisplus", "valaris-plus" -> Tier.VALARIS_PLUS;
            case "free" -> Tier.FREE;
            default -> {
                try {
                    yield Tier.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    yield Tier.FREE;
                }
            }
        };
    }
}
