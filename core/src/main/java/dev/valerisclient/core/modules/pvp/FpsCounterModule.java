package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.i18n.ValerisLang;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.theme.ThemeManager;

/** Framerate counter HUD. */
public final class FpsCounterModule extends Module {

    private final Element element;

    public FpsCounterModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("fps-counter", "FPS Counter", "Shows your framerate", ModuleCategory.PVP);
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

        // Text rebuilt only when the value changes — render stays allocation-free.
        private int lastFps = -1;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("fps", "FPS Counter", HudAnchor.TOP_LEFT, 4, 20);
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
            int fps = adapter.fps();
            if (fps != lastFps) {
                lastFps = fps;
                text = ValerisLang.get("valeris.hud.fps.format", "%d FPS", fps);
            }
        }
    }
}
