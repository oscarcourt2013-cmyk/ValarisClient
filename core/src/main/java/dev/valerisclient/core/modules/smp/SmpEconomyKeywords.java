package dev.valerisclient.core.modules.smp;

import java.util.Locale;

/** Shared economy chat keyword matching. */
final class SmpEconomyKeywords {

    private static final String DEFAULT =
            "buy,sell,trade,ah,auction,shop,balance,money,coin,offer,bid,market";

    private SmpEconomyKeywords() {
    }

    static String defaults() {
        return DEFAULT;
    }

    static boolean matches(String text, String commaSeparated) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String source = commaSeparated == null || commaSeparated.isBlank() ? DEFAULT : commaSeparated;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String part : source.split(",")) {
            String word = part.trim().toLowerCase(Locale.ROOT);
            if (!word.isEmpty() && lower.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
