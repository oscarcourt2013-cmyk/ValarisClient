package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/**
 * Clicks-per-second counter. Press edges are detected per rendered frame and
 * timestamped into fixed ring buffers — no allocation, no tick listener.
 */
public final class CpsCounterModule extends Module {

    private final Element element;

    public CpsCounterModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("cps-counter", "CPS Counter", "Shows your clicks per second", ModuleCategory.PVP);
        this.element = hud.register(new Element(themes, adapter));
        element.setVisible(false);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;
        private static final long WINDOW_MILLIS = 1000;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;
        private final ClickTracker left = new ClickTracker();
        private final ClickTracker right = new ClickTracker();

        private int lastLeft = -1;
        private int lastRight = -1;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("cps", "CPS Counter", HudAnchor.TOP_LEFT, 4, 36);
            this.themes = themes;
            this.adapter = adapter;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return ctx.textWidth(text) + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            left.sample(adapter.isMouseButtonDown(0), nowMillis);
            right.sample(adapter.isMouseButtonDown(1), nowMillis);
            refresh(nowMillis);

            Theme theme = themes.active();
            ctx.fillRect(0, 0, ctx.textWidth(text) + PADDING * 2, measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh(long now) {
            int leftCps = left.countSince(now - WINDOW_MILLIS);
            int rightCps = right.countSince(now - WINDOW_MILLIS);
            if (leftCps != lastLeft || rightCps != lastRight) {
                lastLeft = leftCps;
                lastRight = rightCps;
                text = leftCps + " | " + rightCps + " CPS";
            }
        }

        /** Fixed-size ring buffer of press timestamps with edge detection. */
        private static final class ClickTracker {
            private static final int CAPACITY = 64;

            private final long[] timestamps = new long[CAPACITY];
            private int head;
            private int size;
            private boolean wasDown;

            void sample(boolean down, long now) {
                if (down && !wasDown) {
                    timestamps[head] = now;
                    head = (head + 1) % CAPACITY;
                    if (size < CAPACITY) {
                        size++;
                    }
                }
                wasDown = down;
            }

            int countSince(long cutoff) {
                int count = 0;
                for (int i = 0; i < size; i++) {
                    int index = (head - 1 - i + CAPACITY * 2) % CAPACITY;
                    if (timestamps[index] < cutoff) {
                        break; // older entries only get older
                    }
                    count++;
                }
                return count;
            }
        }
    }
}
