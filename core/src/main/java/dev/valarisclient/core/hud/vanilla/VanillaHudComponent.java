package dev.valarisclient.core.hud.vanilla;

import dev.valarisclient.core.hud.HudAnchor;

/** Vanilla Minecraft HUD regions editable in the HUD editor. */
public enum VanillaHudComponent {
    HOTBAR("vanilla-hotbar", "Hotbar", HudAnchor.BOTTOM_CENTER, 0, 0, 182, 22),
    BOSSBAR("vanilla-bossbar", "Boss Bar", HudAnchor.TOP_CENTER, 0, 12, 182, 19),
    SCOREBOARD("vanilla-scoreboard", "Scoreboard", HudAnchor.MIDDLE_RIGHT, -3, 0, 120, 100),
    STATUS_EFFECTS("vanilla-effects", "Status Effects", HudAnchor.TOP_RIGHT, 0, 1, 125, 51),
    EXPERIENCE("vanilla-experience", "Experience Bar", HudAnchor.BOTTOM_CENTER, 0, -24, 182, 5);

    private final String id;
    private final String label;
    private final HudAnchor defaultAnchor;
    private final float defaultOffsetX;
    private final float defaultOffsetY;
    private final int defaultWidth;
    private final int defaultHeight;

    VanillaHudComponent(String id, String label, HudAnchor defaultAnchor,
                        float defaultOffsetX, float defaultOffsetY,
                        int defaultWidth, int defaultHeight) {
        this.id = id;
        this.label = label;
        this.defaultAnchor = defaultAnchor;
        this.defaultOffsetX = defaultOffsetX;
        this.defaultOffsetY = defaultOffsetY;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public HudAnchor defaultAnchor() {
        return defaultAnchor;
    }

    public float defaultOffsetX() {
        return defaultOffsetX;
    }

    public float defaultOffsetY() {
        return defaultOffsetY;
    }

    public int defaultWidth() {
        return defaultWidth;
    }

    public int defaultHeight() {
        return defaultHeight;
    }
}
