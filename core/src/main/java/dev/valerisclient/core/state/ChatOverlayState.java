package dev.valerisclient.core.state;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Shared chat overlay flags and timestamp formatting, read by version-layer hooks.
 */
public final class ChatOverlayState {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private static boolean enabled;
    private static boolean compact;
    private static boolean highlightMentions;
    private static String mentionPlayerName = "";

    private ChatOverlayState() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean compact() {
        return compact;
    }

    public static void setCompact(boolean value) {
        compact = value;
    }

    public static void setHighlightMentions(boolean value, String playerName) {
        highlightMentions = value;
        mentionPlayerName = playerName != null ? playerName : "";
    }

    /** Prepends a timestamp when {@link #enabled()} is true. Called by hooks before display. */
    public static String formatIncoming(String text, long timestampMillis) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String formatted = text;
        if (enabled) {
            String prefix = "[" + TIME_FORMAT.format(Instant.ofEpochMilli(timestampMillis)) + "]";
            formatted = compact ? prefix + text : prefix + " " + text;
        }
        if (highlightMentions && !mentionPlayerName.isEmpty()
                && text.toLowerCase(java.util.Locale.ROOT).contains(mentionPlayerName.toLowerCase(java.util.Locale.ROOT))) {
            return "§e" + formatted;
        }
        return formatted;
    }
}
