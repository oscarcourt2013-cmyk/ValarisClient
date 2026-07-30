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

/** Attack cooldown progress bar HUD. */
public final class ItemCooldownModule extends Module {

    private final Element element;

    public ItemCooldownModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("item-cooldown", "Item Cooldown", "Shows your attack cooldown progress", ModuleCategory.PVP);
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
        private static final int BAR_WIDTH = 60;
        private static final int BAR_HEIGHT = 4;
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;

        private int lastPercent = -1;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("cooldown", "Item Cooldown", HudAnchor.BOTTOM_CENTER, 0, -20);
            this.themes = themes;
            this.adapter = adapter;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            refresh();
            return Math.max(ctx.textWidth(text) + PADDING * 2, BAR_WIDTH + PADDING * 2);
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() + BAR_HEIGHT + PADDING * 3;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            refresh();
            Theme theme = themes.active();
            int width = measureWidth(ctx);
            int height = measureHeight(ctx);
            ctx.fillRect(0, 0, width, height, theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);

            int barY = PADDING + ctx.fontHeight() + PADDING;
            int barX = PADDING;
            ctx.fillRect(barX, barY, BAR_WIDTH, BAR_HEIGHT, theme.backgroundLight());
            int fillWidth = BAR_WIDTH * lastPercent / 100;
            if (fillWidth > 0) {
                ctx.fillRect(barX, barY, fillWidth, BAR_HEIGHT, theme.accent());
            }
        }

        private void refresh() {
            if (!adapter.hasPlayer()) {
                if (lastPercent != 100) {
                    lastPercent = 100;
                    text = "100%";
                }
                return;
            }
            float cooldown = adapter.attackCooldown();
            int percent = Math.round(Math.clamp(cooldown, 0.0f, 1.0f) * 100);
            if (percent != lastPercent) {
                lastPercent = percent;
                text = percent + "%";
            }
        }
    }
}
