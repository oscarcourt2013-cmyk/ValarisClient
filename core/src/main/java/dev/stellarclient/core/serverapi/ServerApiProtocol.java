package dev.stellarclient.core.serverapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Shared protocol constants and JSON helpers for {@code StellarClient:main}. */
public final class ServerApiProtocol {

    public static final int PROTOCOL = 1;
    public static final String CHANNEL = "StellarClient:main";
    public static final String CLIENT_NAME = "StellarClient";

    public static final String T_HANDSHAKE = "HANDSHAKE";
    public static final String T_SERVER_ACCEPTED = "SERVER_ACCEPTED";
    public static final String T_SERVER_REJECTED = "SERVER_REJECTED";
    public static final String T_ACCOUNT_SYNC = "ACCOUNT_SYNC";
    public static final String T_XP = "XP";
    public static final String T_REWARD = "REWARD";
    public static final String T_NOTIFY = "NOTIFY";
    public static final String T_PROFILE_REQUEST = "PROFILE_REQUEST";
    public static final String T_PROFILE = "PROFILE";

    public static final String XP_PLAYTIME = "SERVER_PLAYTIME";
    public static final String XP_EVENT = "SERVER_EVENT";
    public static final String XP_ACHIEVEMENT = "SERVER_ACHIEVEMENT";

    public static final String REWARD_AVAILABLE = "SERVER_REWARD_AVAILABLE";

    private ServerApiProtocol() {
    }

    public static JsonObject parse(String json) {
        if (json == null || json.isBlank()) {
            return new JsonObject();
        }
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    public static String typeOf(JsonObject obj) {
        if (obj == null || !obj.has("t") || obj.get("t").isJsonNull()) {
            return "";
        }
        return obj.get("t").getAsString();
    }

    public static String str(JsonObject obj, String key, String fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    public static int integer(JsonObject obj, String key, int fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            return fallback;
        }
    }

    public static boolean bool(JsonObject obj, String key, boolean fallback) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }
}
