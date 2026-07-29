package dev.stellarclient.core.gui.component;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.theme.Theme;

/** Horizontal slider with visible value label. */
public final class SliderWidget {

    public void render(RenderContext ctx, Theme theme, int x, int y, int width,
                       String label, String valueText, float fraction, boolean hovered) {
        ctx.drawText(label, x, y, theme.foreground(), true);
        if (valueText != null && !valueText.isEmpty()) {
            ctx.drawText(valueText, x + width - ctx.textWidth(valueText), y, theme.foregroundMuted(), true);
        }
        int barY = y + ctx.fontHeight() + 2;
        int barH = 3;
        ctx.fillRect(x, barY, width, barH, theme.backgroundLight());
        int fill = Math.round(width * Math.clamp(fraction, 0f, 1f));
        ctx.fillRect(x, barY, fill, barH, hovered ? theme.accentSecondary() : theme.accent());
    }

    public float fractionFromMouse(double mouseX, int x, int width) {
        return Math.clamp((float) (mouseX - x) / width, 0f, 1f);
    }
}
