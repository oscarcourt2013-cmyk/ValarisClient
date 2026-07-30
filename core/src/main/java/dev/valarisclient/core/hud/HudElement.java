package dev.valarisclient.core.hud;

import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.i18n.ValarisLang;

/**
 * One movable HUD component (FPS counter, keystrokes, notifications, ...).
 */
public abstract class HudElement {

    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 3.0f;
    public static final float MIN_OPACITY = 0.1f;
    public static final float MAX_OPACITY = 1.0f;

    private final String id;
    private final String name;
    private final HudAnchor defaultAnchor;
    private final float defaultOffsetX;
    private final float defaultOffsetY;

    private HudAnchor anchor;
    private float offsetX;
    private float offsetY;
    private float scale = 1.0f;
    private float rotation;
    private float opacity = 1.0f;
    private int tintArgb;
    private boolean visible = true;

    private float lastX;
    private float lastY;
    private float lastWidth;
    private float lastHeight;

    protected HudElement(String id, String name, HudAnchor defaultAnchor, float defaultOffsetX, float defaultOffsetY) {
        this.id = id;
        this.name = name;
        this.defaultAnchor = defaultAnchor;
        this.defaultOffsetX = defaultOffsetX;
        this.defaultOffsetY = defaultOffsetY;
        this.anchor = defaultAnchor;
        this.offsetX = defaultOffsetX;
        this.offsetY = defaultOffsetY;
    }

    /** Restores the constructor-time layout plus neutral scale, rotation, opacity and tint. */
    public final void resetToDefaults() {
        this.anchor = defaultAnchor;
        this.offsetX = defaultOffsetX;
        this.offsetY = defaultOffsetY;
        this.scale = 1.0f;
        this.rotation = 0f;
        this.opacity = 1.0f;
        this.tintArgb = 0;
        this.visible = true;
    }

    public abstract int measureWidth(RenderContext ctx);

    public abstract int measureHeight(RenderContext ctx);

    public abstract void render(RenderContext ctx, long nowMillis);

    public final String id() {
        return id;
    }

    public final String name() {
        return ValarisLang.hud(id, name);
    }

    public final HudAnchor anchor() {
        return anchor;
    }

    public final float offsetX() {
        return offsetX;
    }

    public final float offsetY() {
        return offsetY;
    }

    public final float scale() {
        return scale;
    }

    public final float rotation() {
        return rotation;
    }

    public final float opacity() {
        return opacity;
    }

    public final int tintArgb() {
        return tintArgb;
    }

    public final boolean isVisible() {
        return visible;
    }

    public final void setVisible(boolean visible) {
        this.visible = visible;
    }

    public final void setLayout(HudAnchor anchor, float offsetX, float offsetY) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public final void setScale(float scale) {
        this.scale = Math.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public final void setRotation(float rotation) {
        this.rotation = rotation % 360f;
    }

    public final void setOpacity(float opacity) {
        this.opacity = Math.clamp(opacity, MIN_OPACITY, MAX_OPACITY);
    }

    public final void setTintArgb(int tintArgb) {
        this.tintArgb = tintArgb;
    }

    final void setLastBounds(float x, float y, float width, float height) {
        this.lastX = x;
        this.lastY = y;
        this.lastWidth = width;
        this.lastHeight = height;
    }

    public final float lastX() {
        return lastX;
    }

    public final float lastY() {
        return lastY;
    }

    public final float lastWidth() {
        return lastWidth;
    }

    public final float lastHeight() {
        return lastHeight;
    }

    public final boolean containsPoint(double x, double y) {
        return x >= lastX && x < lastX + lastWidth && y >= lastY && y < lastY + lastHeight;
    }
}
