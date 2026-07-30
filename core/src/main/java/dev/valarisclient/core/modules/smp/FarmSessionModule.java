package dev.valarisclient.core.modules.smp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.DoubleSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/** Tracks farm session duration and a manual profit counter. */
public final class FarmSessionModule extends Module {

    private final DoubleSetting sessionProfit = addSetting(new DoubleSetting(
            "session-profit", "Session profit", "Manual profit counter for this session", 0, 0, 1_000_000_000));

    private final Element element;
    private final MinecraftAdapter adapter;

    public FarmSessionModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("farm-session", "Farm Session", "Session time and manual profit tracker", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> refresh());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void refresh() {
        long millis = adapter.sessionMillis();
        int min = (int) (millis / 60_000);
        int sec = (int) ((millis / 1000) % 60);
        element.setText(String.format("Farm: %d:%02d | +%s",
                min, sec, ChestValueModule.formatMoney(sessionProfit.get())));
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private String text = "Farm: 0:00 | +0";

        Element(ThemeManager themes) {
            super("farm-session", "Farm Session", HudAnchor.TOP_LEFT, 4, 92);
            this.themes = themes;
        }

        void setText(String text) {
            this.text = text;
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
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }
    }
}
