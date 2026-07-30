package dev.valarisclient.core.gui.menu;

/** Allows opening the vanilla {@code TitleScreen} once without Valaris redirect. */
public final class TitleScreenGate {

    private static boolean allowVanilla;

    private TitleScreenGate() {
    }

    public static void requestVanilla() {
        allowVanilla = true;
    }

    /** Returns {@code true} once when vanilla title init should proceed normally. */
    public static boolean consumeAllowVanilla() {
        if (!allowVanilla) {
            return false;
        }
        allowVanilla = false;
        return true;
    }
}
