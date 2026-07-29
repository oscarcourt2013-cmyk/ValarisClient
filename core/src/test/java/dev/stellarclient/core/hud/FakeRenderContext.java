package dev.stellarclient.core.hud;

import dev.stellarclient.core.adapter.RenderContext;

/** Headless {@link RenderContext} for tests: 6px-wide glyphs, 9px lines. */
public final class FakeRenderContext implements RenderContext {

    private final int width;
    private final int height;
    public int fillCalls;
    public int textCalls;
    private int clipDepth;

    public FakeRenderContext(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public int screenWidth() {
        return width;
    }

    @Override
    public int screenHeight() {
        return height;
    }

    @Override
    public void fillRect(int x, int y, int w, int h, int argb) {
        fillCalls++;
    }

    @Override
    public void drawText(String text, int x, int y, int argb, boolean shadow) {
        textCalls++;
    }

    @Override
    public void drawUiText(String text, int x, int y, int argb) {
        textCalls++;
    }

    @Override
    public int uiTextWidth(String text) {
        return text.length() * 6;
    }

    @Override
    public int textWidth(String text) {
        return text.length() * 6;
    }

    @Override
    public int fontHeight() {
        return 9;
    }

    @Override
    public void pushTransform(float translateX, float translateY, float scale,
                              float rotationDegrees, float pivotLocalX, float pivotLocalY) {
    }

    @Override
    public void setDrawOpacity(float opacity) {
    }

    @Override
    public void popTransform() {
    }

    @Override
    public void pushClip(int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            clipDepth++;
        }
    }

    @Override
    public void popClip() {
        if (clipDepth > 0) {
            clipDepth--;
        }
    }

    public int clipDepth() {
        return clipDepth;
    }
}
