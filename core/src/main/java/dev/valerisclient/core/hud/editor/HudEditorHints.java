package dev.valerisclient.core.hud.editor;

/** Short HUD editor footer hints. Kept terse so they fit a 320px-wide canvas. */
public final class HudEditorHints {

    public static final String LINE_1 =
            "Drag to move · Arrows nudge · Scroll scale · H hides panels · Esc closes";
    public static final String LINE_2 = "";

    /** Shown while the chrome is hidden, so the way back is always on screen. */
    public static final String HIDDEN = "Panels hidden — H to show · Esc closes";

    private HudEditorHints() {
    }
}
