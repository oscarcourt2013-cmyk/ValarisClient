package dev.stellarclient.core.cosmetics;

/** Equipped cape + wings IDs (empty strings mean none). */
public record CosmeticLoadout(String capeId, String wingsId) {

    public static final CosmeticLoadout EMPTY = new CosmeticLoadout("", "");

    public CosmeticLoadout {
        capeId = capeId == null ? "" : capeId;
        wingsId = wingsId == null ? "" : wingsId;
    }

    public boolean hasCape() {
        return !capeId.isBlank();
    }

    public boolean hasWings() {
        return !wingsId.isBlank();
    }
}
