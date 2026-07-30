package dev.valarisclient.core.modules.smp;

import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.IntSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/** Manual auction countdown timer HUD. Resets when enabled. */
public final class AuctionTimerModule extends Module {

    private final IntSetting durationMinutes = addSetting(new IntSetting(
            "duration-minutes", "Duration", "Countdown length in minutes", 5, 1, 120));

    private final Element element;
    private long endMillis;

    public AuctionTimerModule(HudManager hud, ThemeManager themes) {
        super("auction-timer", "Auction Timer", "Manual countdown for auction sniping", ModuleCategory.QOL);
        this.element = hud.register(new Element(themes));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> tick());
    }

    @Override
    protected void onEnable() {
        endMillis = System.currentTimeMillis() + durationMinutes.get() * 60_000L;
        element.setVisible(true);
        updateText();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void tick() {
        updateText();
    }

    private void updateText() {
        long remaining = Math.max(0, endMillis - System.currentTimeMillis());
        int totalSec = (int) (remaining / 1000);
        int min = totalSec / 60;
        int sec = totalSec % 60;
        element.setText(String.format("Auction: %d:%02d", min, sec));
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private String text = "Auction: 0:00";

        Element(ThemeManager themes) {
            super("auction-timer", "Auction Timer", HudAnchor.TOP_CENTER, 0, 36);
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
