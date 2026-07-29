package dev.stellarclient.core.design;

/**
 * StellarClient v1.1 design tokens â€” spacing, radii, motion, typography scale.
 * Colors live on {@link dev.stellarclient.core.theme.Theme}; this class holds layout and motion.
 */
public final class PrimeDesign {

    /** Synced from {@code gradle.properties} {@code mod_version} at build time. */
    public static final String VERSION = PrimeVersion.VERSION;
    public static final String TAGLINE = "Premium Minecraft Client";

    // Spacing (px, GUI scaled)
    public static final int SPACE_XS = 2;
    public static final int SPACE_SM = 4;
    public static final int SPACE_MD = 8;
    public static final int SPACE_LG = 12;
    public static final int SPACE_XL = 16;
    public static final int SPACE_2XL = 24;

    // Radii
    public static final int RADIUS_SM = 2;
    public static final int RADIUS_MD = 4;
    public static final int RADIUS_LG = 8;

    // Motion (seconds-ish lerp factors are applied per tick in UiAnimator)
    public static final float MOTION_FAST = 18f;
    public static final float MOTION_NORMAL = 12f;
    public static final float MOTION_SLOW = 8f;

    // Component sizes
    public static final int ROW_HEIGHT = 16;
    public static final int TOGGLE_WIDTH = 22;
    public static final int TOGGLE_HEIGHT = 11;
    public static final int INPUT_HEIGHT = 14;
    public static final int CARD_MIN_HEIGHT = 52;
    public static final int MENU_BUTTON_HEIGHT = 22;

    // HUD Editor
    public static final int GRID_SIZE = 8;
    public static final int SNAP_THRESHOLD = 4;

    private PrimeDesign() {
    }
}
