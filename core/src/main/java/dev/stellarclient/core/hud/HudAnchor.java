package dev.stellarclient.core.hud;

/**
 * The nine screen anchor points a HUD element can attach to.
 *
 * <p>An element stores an anchor plus a pixel offset instead of absolute
 * coordinates: layouts survive resolution and GUI-scale changes, and elements
 * anchored right/bottom stay glued to their edge.</p>
 */
public enum HudAnchor {
    TOP_LEFT(0, 0),
    TOP_CENTER(1, 0),
    TOP_RIGHT(2, 0),
    MIDDLE_LEFT(0, 1),
    CENTER(1, 1),
    MIDDLE_RIGHT(2, 1),
    BOTTOM_LEFT(0, 2),
    BOTTOM_CENTER(1, 2),
    BOTTOM_RIGHT(2, 2);

    private final int horizontal; // 0 left, 1 center, 2 right
    private final int vertical;   // 0 top,  1 middle, 2 bottom

    HudAnchor(int horizontal, int vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    /** Base X of an element of {@code elementWidth} aligned to this anchor. */
    public float baseX(int screenWidth, float elementWidth) {
        return switch (horizontal) {
            case 0 -> 0;
            case 1 -> (screenWidth - elementWidth) / 2f;
            default -> screenWidth - elementWidth;
        };
    }

    /** Base Y of an element of {@code elementHeight} aligned to this anchor. */
    public float baseY(int screenHeight, float elementHeight) {
        return switch (vertical) {
            case 0 -> 0;
            case 1 -> (screenHeight - elementHeight) / 2f;
            default -> screenHeight - elementHeight;
        };
    }

    /**
     * The anchor whose screen third contains the point
     * ({@code centerXFraction}, {@code centerYFraction}) — both in [0, 1].
     * Used by the HUD editor to re-anchor an element after a drag.
     */
    public static HudAnchor closest(float centerXFraction, float centerYFraction) {
        int h = centerXFraction < 1f / 3 ? 0 : (centerXFraction < 2f / 3 ? 1 : 2);
        int v = centerYFraction < 1f / 3 ? 0 : (centerYFraction < 2f / 3 ? 1 : 2);
        for (HudAnchor anchor : values()) {
            if (anchor.horizontal == h && anchor.vertical == v) {
                return anchor;
            }
        }
        throw new AssertionError();
    }
}
