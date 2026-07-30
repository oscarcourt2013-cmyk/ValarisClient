package dev.stellarclient.core.hud.editor;

/** True while the HUD editor screen is open — used to defer in-game HUD rendering. */
public final class HudEditorState {

    private static boolean active;
    private static int vanillaRenderDepth;

    private HudEditorState() {
    }

    public static void setActive(boolean value) {
        active = value;
        if (!value) {
            vanillaRenderDepth = 0;
        }
    }

    public static boolean isActive() {
        return active;
    }

    /** True while {@code HudEditorScreen} is intentionally re-drawing vanilla HUD. */
    public static boolean isRenderingVanillaHud() {
        return vanillaRenderDepth > 0;
    }

    public static void runVanillaHudRender(Runnable render) {
        vanillaRenderDepth++;
        try {
            render.run();
        } finally {
            if (vanillaRenderDepth > 0) {
                vanillaRenderDepth--;
            }
        }
    }
}
