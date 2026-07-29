package dev.stellarclient.core.cosmetics;

import java.util.Locale;
import java.util.Map;

/** Maps catalog cosmetic IDs to asset paths under {@code assets/stellarclient/}. */
public final class CosmeticTextures {

    private static final Map<String, String> CAPE_PATHS = Map.of(
            "cape-prime", "textures/cosmetics/cape_prime.png",
            "cape-star", "textures/cosmetics/cape_star.png",
            "cape-crimson", "textures/cosmetics/cape_crimson.png",
            "cape-midnight", "textures/cosmetics/cape_midnight.png");

    private static final Map<String, String> WINGS_PATHS = Map.of(
            "wings-ember", "textures/cosmetics/wings_ember.png",
            "wings-aurora", "textures/cosmetics/wings_aurora.png",
            // Legacy alias from older bridge / configs
            "wings-light", "textures/cosmetics/wings_aurora.png");

    private CosmeticTextures() {
    }

    public static String capePath(String capeId) {
        if (capeId == null || capeId.isBlank()) {
            return null;
        }
        return CAPE_PATHS.get(capeId.toLowerCase(Locale.ROOT));
    }

    public static String wingsPath(String wingsId) {
        if (wingsId == null || wingsId.isBlank()) {
            return null;
        }
        return WINGS_PATHS.get(wingsId.toLowerCase(Locale.ROOT));
    }

    public static boolean isKnownCape(String capeId) {
        return capePath(capeId) != null;
    }

    public static boolean isKnownWings(String wingsId) {
        return wingsPath(wingsId) != null;
    }
}
