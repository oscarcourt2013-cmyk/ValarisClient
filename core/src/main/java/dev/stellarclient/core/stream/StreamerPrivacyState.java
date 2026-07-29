package dev.stellarclient.core.stream;

/** Shared stream-privacy flags read by modules, HUD elements, and version-layer mixins. */
public final class StreamerPrivacyState {

    private static boolean debugShield;
    private static boolean chatRedact;
    private static boolean nameMask;
    private static boolean hudShield;
    private static boolean brandingHide;
    private static boolean blockLocationHud;

    private static boolean redactCoords = true;
    private static boolean redactIps = true;
    private static boolean redactWhispers = true;

    private static boolean maskSelf;

    private StreamerPrivacyState() {
    }

    public static boolean debugShield() {
        return debugShield;
    }

    public static void setDebugShield(boolean value) {
        debugShield = value;
    }

    public static boolean chatRedact() {
        return chatRedact;
    }

    public static void setChatRedact(boolean value) {
        chatRedact = value;
    }

    public static boolean nameMask() {
        return nameMask;
    }

    public static void setNameMask(boolean value) {
        nameMask = value;
    }

    public static boolean hudShield() {
        return hudShield;
    }

    public static void setHudShield(boolean value) {
        hudShield = value;
        if (value) {
            blockLocationHud = true;
        }
    }

    public static boolean brandingHide() {
        return brandingHide;
    }

    public static void setBrandingHide(boolean value) {
        brandingHide = value;
    }

    public static boolean blockLocationHud() {
        return blockLocationHud || hudShield;
    }

    public static void setBlockLocationHud(boolean value) {
        blockLocationHud = value;
    }

    public static boolean redactCoords() {
        return redactCoords;
    }

    public static void setRedactCoords(boolean value) {
        redactCoords = value;
    }

    public static boolean redactIps() {
        return redactIps;
    }

    public static void setRedactIps(boolean value) {
        redactIps = value;
    }

    public static boolean redactWhispers() {
        return redactWhispers;
    }

    public static void setRedactWhispers(boolean value) {
        redactWhispers = value;
    }

    public static boolean maskSelf() {
        return maskSelf;
    }

    public static void setMaskSelf(boolean value) {
        maskSelf = value;
    }

    /** Clears all flags â€” for tests and shutdown. */
    public static void reset() {
        debugShield = false;
        chatRedact = false;
        nameMask = false;
        hudShield = false;
        brandingHide = false;
        blockLocationHud = false;
        redactCoords = true;
        redactIps = true;
        redactWhispers = true;
        maskSelf = false;
    }
}
