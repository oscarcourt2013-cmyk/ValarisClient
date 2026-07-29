package dev.stellarclient.core.modules.performance;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Live JVM memory usage HUD. */
public final class MemoryMonitorModule extends Module {

    private final Element element;

    public MemoryMonitorModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("memory-monitor", "Memory Monitor", "Shows used and max memory", ModuleCategory.PERFORMANCE);
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

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;

        private long lastUsed = -1;
        private long lastMax = -1;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("memory", "Memory Monitor", HudAnchor.TOP_LEFT, 4, 52);
            this.themes = themes;
            this.adapter = adapter;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            refresh();
            return ctx.textWidth(text) + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh() {
            long used = adapter.usedMemoryMb();
            long max = adapter.maxMemoryMb();
            if (used != lastUsed || max != lastMax) {
                lastUsed = used;
                lastMax = max;
                text = used + " / " + max + " MB";
            }
        }
    }
}
