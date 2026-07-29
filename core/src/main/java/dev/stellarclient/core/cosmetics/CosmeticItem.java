package dev.stellarclient.core.cosmetics;

/** One cosmetic item with rarity for future shop/inventory. */
public record CosmeticItem(
        String id,
        String name,
        CosmeticType type,
        Rarity rarity,
        int tintArgb
) {
    public enum Rarity {
        COMMON, RARE, EPIC, LEGENDARY
    }
}
