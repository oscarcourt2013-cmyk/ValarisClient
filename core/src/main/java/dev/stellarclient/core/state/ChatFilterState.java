package dev.stellarclient.core.state;

/**
 * Shared chat filter word list, read by version-layer hooks.
 */
public final class ChatFilterState {

    private static boolean enabled;
    private static String[] words = new String[0];

    private ChatFilterState() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void setWords(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            words = new String[0];
            return;
        }
        String[] raw = commaSeparated.split(",");
        int count = 0;
        for (String part : raw) {
            if (!part.isBlank()) {
                count++;
            }
        }
        if (count == 0) {
            words = new String[0];
            return;
        }
        String[] parsed = new String[count];
        int index = 0;
        for (String part : raw) {
            String trimmed = part.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                parsed[index++] = trimmed;
            }
        }
        words = parsed;
    }

    public static boolean shouldFilter(String text) {
        if (!enabled || words.length == 0 || text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase();
        for (String word : words) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
