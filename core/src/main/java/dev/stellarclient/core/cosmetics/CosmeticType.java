package dev.stellarclient.core.cosmetics;

/** Cosmetic slot types. Only CAPE and WINGS are equippable in-world. */
public enum CosmeticType {
    CAPE,
    WINGS,
    @Deprecated HAT,
    @Deprecated PARTICLES,
    /** Launcher profile badges only â€” not world-rendered. */
    BADGE
}
