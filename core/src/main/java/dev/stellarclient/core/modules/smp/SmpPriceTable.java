package dev.stellarclient.core.modules.smp;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Parses {@code item:price} lists for economy helpers. */
final class SmpPriceTable {

    private final Map<String, Double> prices;

    private SmpPriceTable(Map<String, Double> prices) {
        this.prices = prices;
    }

    static SmpPriceTable parse(String config) {
        Map<String, Double> map = new HashMap<>();
        if (config == null || config.isBlank()) {
            return new SmpPriceTable(map);
        }
        for (String part : config.split(",")) {
            int colon = part.indexOf(':');
            if (colon <= 0 || colon >= part.length() - 1) {
                continue;
            }
            String key = normalizeKey(part.substring(0, colon));
            try {
                map.put(key, Double.parseDouble(part.substring(colon + 1).trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return new SmpPriceTable(map);
    }

    double lookup(String itemName) {
        if (itemName == null || itemName.isEmpty() || prices.isEmpty()) {
            return 0;
        }
        Double exact = prices.get(normalizeKey(itemName));
        if (exact != null) {
            return exact;
        }
        String lower = normalizeKey(itemName);
        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            if (lower.contains(entry.getKey()) || entry.getKey().contains(lower)) {
                return entry.getValue();
            }
        }
        return 0;
    }

    boolean isEmpty() {
        return prices.isEmpty();
    }

    private static String normalizeKey(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
