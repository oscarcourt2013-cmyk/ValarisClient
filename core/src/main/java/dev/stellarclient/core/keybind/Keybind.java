package dev.stellarclient.core.keybind;

/**
 * A rebindable key action, identified by GLFW key code.
 *
 * <p>GLFW codes are stable across Minecraft versions, so keybinds live
 * entirely in the core; version layers only forward raw key events.</p>
 */
public final class Keybind {

    /** No key assigned. */
    public static final int UNBOUND = -1;

    private final String id;
    private final String displayName;
    private final String category;
    private final int defaultKey;

    private int key;
    private boolean pressed;
    private Runnable pressAction;
    private Runnable releaseAction;

    /**
     * @param id         stable identifier used in configs, e.g. {@code "zoom"}
     * @param defaultKey GLFW key code or {@link #UNBOUND}
     */
    public Keybind(String id, String displayName, String category, int defaultKey) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.defaultKey = defaultKey;
        this.key = defaultKey;
    }

    public Keybind onPress(Runnable action) {
        this.pressAction = action;
        return this;
    }

    public Keybind onRelease(Runnable action) {
        this.releaseAction = action;
        return this;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String category() {
        return category;
    }

    public int defaultKey() {
        return defaultKey;
    }

    public int key() {
        return key;
    }

    public boolean isPressed() {
        return pressed;
    }

    public boolean isBound() {
        return key != UNBOUND;
    }

    /** Package-private: rebinding goes through {@link KeybindManager} so its key index stays consistent. */
    void setKey(int key) {
        this.key = key;
    }

    void handle(boolean down) {
        if (this.pressed == down) {
            return;
        }
        this.pressed = down;
        Runnable action = down ? pressAction : releaseAction;
        if (action != null) {
            action.run();
        }
    }
}
