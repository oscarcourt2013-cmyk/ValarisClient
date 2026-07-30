package dev.valarisclient.core.gui.component;

import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.design.ValarisDesign;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.util.ColorUtil;
import dev.valarisclient.core.util.Easing;

/** Animated on/off toggle switch. */
public final class ToggleWidget {

    private float knobProgress = 0f;

    public void tick(boolean on, float deltaSeconds) {
        knobProgress = Easing.lerp(knobProgress, on ? 1f : 0f, deltaSeconds * ValarisDesign.MOTION_FAST);
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, boolean on) {
        int w = ValarisDesign.TOGGLE_WIDTH;
        int h = ValarisDesign.TOGGLE_HEIGHT;
        int radius = h / 2;
        int track = on ? theme.accent() : ColorUtil.withAlpha(theme.backgroundLight(), 0.95f);
        ctx.fillRoundedRect(x, y, w, h, radius, track);
        if (on) {
            ctx.fillGradientHorizontal(x + 1, y + 1, w - 2, h - 2,
                    ColorUtil.withAlpha(theme.accent(), 0.95f),
                    ColorUtil.withAlpha(theme.accentSecondary(), 0.85f));
        }
        int knob = h - 4;
        int knobX = x + 2 + Math.round((w - knob - 4) * knobProgress);
        int knobY = y + 2;
        ctx.fillRoundedRect(knobX, knobY, knob, knob, knob / 2, 0xFFFFFFFF);
    }

    public boolean hit(double mx, double my, int x, int y) {
        return mx >= x && mx < x + ValarisDesign.TOGGLE_WIDTH
                && my >= y && my < y + ValarisDesign.TOGGLE_HEIGHT;
    }
}
