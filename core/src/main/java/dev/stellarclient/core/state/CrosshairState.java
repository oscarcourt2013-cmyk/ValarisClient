package dev.stellarclient.core.state;

import dev.stellarclient.core.crosshair.CrosshairConfig;

/** Bridge state read by version-layer mixins (hide vanilla crosshair). */
public final class CrosshairState {

    private static CrosshairConfig activeConfig = new CrosshairConfig();
    private static boolean moduleEnabled;

    private CrosshairState() {
    }

    public static void setActive(CrosshairConfig config, boolean enabled) {
        activeConfig = config;
        moduleEnabled = enabled;
    }

    public static boolean hideVanillaCrosshair() {
        return moduleEnabled && activeConfig.hideVanilla;
    }

    public static CrosshairConfig config() {
        return activeConfig;
    }
}
