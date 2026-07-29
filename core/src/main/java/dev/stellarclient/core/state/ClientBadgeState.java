package dev.stellarclient.core.state;

import dev.stellarclient.core.account.PrimeAccountService;

/** Whether the StellarClient tab-list badge module is active, plus tier tint. */
public final class ClientBadgeState {

    private static boolean active;
    private static int background = 0xFF14141C;
    private static int accent = 0xFFE11D48;
    private static int foreground = 0xFFFF8A9A;

    private ClientBadgeState() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static int background() {
        return background;
    }

    public static int accent() {
        return accent;
    }

    public static int foreground() {
        return foreground;
    }

    /** Tints the badge plate by account grade (free / prime / prime_plus). */
    public static void setTier(PrimeAccountService.Tier tier) {
        PrimeAccountService.Tier resolved = tier != null ? tier : PrimeAccountService.Tier.FREE;
        switch (resolved) {
            case FREE -> {
                background = 0xFF1A1A22;
                accent = 0xFF6B7280;
                foreground = 0xFFD1D5DB;
            }
            case PRIME_PLUS -> {
                background = 0xFF1A1610;
                accent = 0xFFF59E0B;
                foreground = 0xFFFDE68A;
            }
            case PREMIUM -> {
                background = 0xFF14141C;
                accent = 0xFFE11D48;
                foreground = 0xFFFF8A9A;
            }
        }
    }

    public static void reset() {
        active = false;
        setTier(PrimeAccountService.Tier.PREMIUM);
    }
}
