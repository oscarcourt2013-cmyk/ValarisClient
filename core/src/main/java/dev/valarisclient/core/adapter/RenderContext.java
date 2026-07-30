package dev.valarisclient.core.adapter;

/**
 * Version-independent 2D drawing surface for HUD and GUI rendering.
 *
 * <p>Backed by {@code GuiGraphics} on 1.21.11 and {@code GuiGraphicsExtractor}
 * on 26.2. One instance is reused every frame — implementations must be
 * stateless between {@code prepare} calls and allocation-free during drawing
 * (except while GUI screens are open).</p>
 */
public interface RenderContext {

    int screenWidth();

    int screenHeight();

    void fillRect(int x, int y, int width, int height, int argb);

    void drawText(String text, int x, int y, int argb, boolean shadow);

    /** Shadowless text with optional uniform scale — cleaner than vanilla MC labels. */
    default void drawSmoothText(String text, int x, int y, int argb, float scale) {
        if (Math.abs(scale - 1f) < 0.01f) {
            drawUiText(text, x, y, argb);
            return;
        }
        pushTransform(x, y, scale);
        drawUiText(text, 0, 0, argb);
        popTransform();
    }

    default int smoothTextWidth(String text, float scale) {
        return Math.round(uiTextWidth(text) * scale);
    }

    /** Premium TTF UI labels (Inter). Falls back to vanilla in headless tests. */
    default void drawUiText(String text, int x, int y, int argb) {
        drawText(text, x, y, argb, false);
    }

    default int uiTextWidth(String text) {
        return textWidth(text);
    }

    default int uiFontHeight() {
        return fontHeight();
    }

    int textWidth(String text);

    int fontHeight();

    /** Pushes translation + uniform scale around local origin. */
    default void pushTransform(float translateX, float translateY, float scale) {
        pushTransform(translateX, translateY, scale, 0f, 0f, 0f);
    }

    /**
     * Pushes translation, rotation (degrees) and scale around a local pivot.
     * Draw calls after this should use coordinates relative to the pivot.
     */
    void pushTransform(float translateX, float translateY, float scale,
                       float rotationDegrees, float pivotLocalX, float pivotLocalY);

    void popTransform();

    /** Multiplies alpha on subsequent draw calls until the next {@link #setDrawOpacity(float)}. */
    void setDrawOpacity(float opacity);

    /** Clips subsequent draw calls to the rectangle (screen space). No-op in headless tests. */
    default void pushClip(int x, int y, int width, int height) {
    }

    /** Ends the innermost clip region opened by {@link #pushClip(int, int, int, int)}. */
    default void popClip() {
    }

    /**
     * Draws a mod GUI texture. Path is relative to {@code assets/<modid>/},
     * e.g. {@code textures/gui/logo.png} for {@code assets/<modid>/textures/gui/logo.png}.
     */
    default void drawTexture(String texturePath, int x, int y, int width, int height,
                             int textureWidth, int textureHeight, int tintArgb) {
    }

    /**
     * Draws the equipped armor item icon for adapter slot
     * ({@code 0}=boots, {@code 1}=leggings, {@code 2}=chest, {@code 3}=helmet).
     * No-op when empty or no player. Coordinates are local to the current transform.
     */
    default void drawArmorItem(int slot, int x, int y) {
    }

    /**
     * Draws a faint ghost placeholder for an empty armor slot (same slot index as
     * {@link #drawArmorItem(int, int, int)}).
     */
    default void drawArmorGhost(int slot, int x, int y) {
    }

    /** Vertical gradient fill (top → bottom). Chunked — not one draw call per row. */
    default void fillGradientVertical(int x, int y, int width, int height, int topArgb, int bottomArgb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int steps = Math.min(height, 24);
        int cursor = 0;
        for (int i = 0; i < steps; i++) {
            int next = (i + 1) * height / steps;
            float t = i / (float) Math.max(1, steps - 1);
            fillRect(x, y + cursor, width, next - cursor,
                    dev.valarisclient.core.util.ColorUtil.lerp(topArgb, bottomArgb, t));
            cursor = next;
        }
    }

    /** Horizontal gradient fill (left → right). Chunked — not one draw call per column. */
    default void fillGradientHorizontal(int x, int y, int width, int height, int leftArgb, int rightArgb) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int steps = Math.min(width, 24);
        int cursor = 0;
        for (int i = 0; i < steps; i++) {
            int next = (i + 1) * width / steps;
            float t = i / (float) Math.max(1, steps - 1);
            fillRect(x + cursor, y, next - cursor, height,
                    dev.valarisclient.core.util.ColorUtil.lerp(leftArgb, rightArgb, t));
            cursor = next;
        }
    }

    default void fillRoundedRect(int x, int y, int width, int height, int radius, int argb) {
        dev.valarisclient.core.gui.RoundedRect.fill(this, x, y, width, height, radius, argb);
    }

    default void fillRoundedBorder(int x, int y, int width, int height, int radius,
                                   int thickness, int borderArgb, int innerArgb) {
        dev.valarisclient.core.gui.RoundedRect.border(this, x, y, width, height, radius, thickness, borderArgb, innerArgb);
    }

    default void fillSoftShadow(int x, int y, int width, int height, int radius, int shadowArgb) {
        dev.valarisclient.core.gui.RoundedRect.softShadow(this, x, y, width, height, radius, shadowArgb);
    }
}
