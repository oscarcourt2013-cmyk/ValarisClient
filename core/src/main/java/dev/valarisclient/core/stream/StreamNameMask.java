package dev.valarisclient.core.stream;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Stable per-session aliases for player names shown on stream. */
public final class StreamNameMask {

    private static final Map<String, String> CACHE = new HashMap<>();
    private static final long SESSION_SEED = System.nanoTime();

    private StreamNameMask() {
    }

    public static String maskPlayerName(String realName) {
        if (realName == null || realName.isEmpty()) {
            return "Player_0000";
        }
        return CACHE.computeIfAbsent(realName, StreamNameMask::aliasFor);
    }

    public static void clearSession() {
        CACHE.clear();
    }

    private static String aliasFor(String realName) {
        int hash = (realName.hashCode() ^ (int) SESSION_SEED ^ (int) (SESSION_SEED >>> 32)) & 0xFFFF;
        return "Player_" + String.format(Locale.ROOT, "%04x", hash);
    }
}
