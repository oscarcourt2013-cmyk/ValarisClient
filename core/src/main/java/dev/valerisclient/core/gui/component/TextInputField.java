package dev.valerisclient.core.gui.component;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.design.ValerisDesign;
import dev.valerisclient.core.gui.GuiLayout;
import dev.valerisclient.core.theme.Theme;

/**
 * Single-line text field with cursor, backspace and character limit.
 * Allocation-free during typing (reuses {@link StringBuilder}).
 */
public final class TextInputField {

    private final StringBuilder buffer = new StringBuilder();
    private final String placeholder;
    private final int maxLength;
    private boolean focused;
    private int cursor;
    private long blinkMillis;

    public TextInputField(String initial, String placeholder, int maxLength) {
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLength = Math.max(1, maxLength);
        setText(initial);
    }

    public void setText(String text) {
        buffer.setLength(0);
        if (text != null) {
            int len = Math.min(text.length(), maxLength);
            buffer.append(text, 0, len);
        }
        cursor = buffer.length();
    }

    public String text() {
        return buffer.toString();
    }

    public boolean focused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            blinkMillis = System.currentTimeMillis();
        }
    }

    public boolean charTyped(char c) {
        if (!focused || c < ' ' || buffer.length() >= maxLength) {
            return false;
        }
        buffer.insert(cursor, c);
        cursor++;
        blinkMillis = System.currentTimeMillis();
        return true;
    }

    public boolean keyPressed(int glfwKey) {
        if (!focused) {
            return false;
        }
        if (glfwKey == 259) { // Backspace
            if (cursor > 0 && !buffer.isEmpty()) {
                buffer.deleteCharAt(cursor - 1);
                cursor--;
                blinkMillis = System.currentTimeMillis();
            }
            return true;
        }
        if (glfwKey == 261) { // Delete
            if (cursor < buffer.length()) {
                buffer.deleteCharAt(cursor);
                blinkMillis = System.currentTimeMillis();
            }
            return true;
        }
        if (glfwKey == 263 && cursor > 0) { // Left
            cursor--;
            blinkMillis = System.currentTimeMillis();
            return true;
        }
        if (glfwKey == 262 && cursor < buffer.length()) { // Right
            cursor++;
            blinkMillis = System.currentTimeMillis();
            return true;
        }
        if (glfwKey == 268) { // Home
            cursor = 0;
            return true;
        }
        if (glfwKey == 269) { // End
            cursor = buffer.length();
            return true;
        }
        return false;
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, int width) {
        int h = ValerisDesign.INPUT_HEIGHT;
        ctx.fillRoundedRect(x, y, width, h, ValerisDesign.RADIUS_SM,
                focused ? theme.surfaceElevated() : theme.backgroundLight());
        ctx.fillGradientHorizontal(x + 1, y + h - 2, width - 2, 2,
                focused ? theme.accent() : theme.border(),
                dev.valerisclient.core.util.ColorUtil.withAlpha(theme.accent(), 0.05f));

        String display = buffer.isEmpty() ? placeholder : buffer.toString();
        int color = buffer.isEmpty() ? theme.foregroundMuted() : theme.foreground();
        int textY = y + (h - ctx.uiFontHeight()) / 2 + 1;
        int textX = x + ValerisDesign.SPACE_SM;
        int maxTextW = width - ValerisDesign.SPACE_SM * 2;
        ctx.drawUiText(trimToWidth(ctx, display, maxTextW), textX, textY, color);

        if (focused && (System.currentTimeMillis() - blinkMillis) % 1000 < 500) {
            String before = buffer.substring(0, cursor);
            int cx = textX + ctx.uiTextWidth(trimToWidth(ctx, before, maxTextW));
            ctx.fillRect(cx, textY - 1, 1, ctx.uiFontHeight() + 1, theme.accent());
        }
    }

    public boolean hit(double mx, double my, int x, int y, int width) {
        return mx >= x && mx < x + width && my >= y && my < y + ValerisDesign.INPUT_HEIGHT;
    }

    private static String trimToWidth(RenderContext ctx, String text, int maxWidth) {
        return GuiLayout.trimToWidth(ctx, text, maxWidth);
    }
}
