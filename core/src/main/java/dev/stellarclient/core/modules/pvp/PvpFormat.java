package dev.stellarclient.core.modules.pvp;

/** Shared formatting for PvP HUD modules. */
final class PvpFormat {

    private PvpFormat() {
    }

    static String cooldown(String label, float ready) {
        if (ready >= 0.99f) {
            return label + ": READY";
        }
        return label + ": " + (int) (ready * 100) + "%";
    }

    static String maceTier(float fall) {
        if (fall >= 5f) {
            return "MAX";
        }
        if (fall >= 3f) {
            return "HIGH";
        }
        if (fall >= 1.5f) {
            return "MID";
        }
        if (fall > 0.05f) {
            return "LOW";
        }
        return "â€”";
    }
}
