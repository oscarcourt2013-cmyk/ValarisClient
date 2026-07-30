package dev.valerisclient.core.gui;

/** Tracks whether the current frame has a live blurred game-world backdrop. */
public final class BlurBackdrop {

    private static boolean active;

    private BlurBackdrop() {
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static boolean isActive() {
        return active;
    }
}
