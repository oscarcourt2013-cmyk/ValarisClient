package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.theme.ThemeManager;

/** Shows the hovered item name in a HUD overlay. */
public final class BetterTooltipsModule extends Module {

    private final BooleanSetting showName =
            addSetting(new BooleanSetting("show-name", "Show name", "Display hovered item name", true));

    private final Element element;

    public BetterTooltipsModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("better-tooltips", "Better Tooltips", "Shows hovered item details in HUD", ModuleCategory.QOL);
        this.element = hud.register(new Element(themes, adapter, showName));
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
        private final BooleanSetting showName;

        private String lastName = "";
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter, BooleanSetting showName) {
            super("better-tooltips", "Better Tooltips", HudAnchor.BOTTOM_CENTER, 0, -40);
            this.themes = themes;
            this.adapter = adapter;
            this.showName = showName;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            refresh();
            if (text.isEmpty()) {
                return 0;
            }
            return ctx.textWidth(text) + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            refresh();
            if (text.isEmpty()) {
                return 0;
            }
            return ctx.fontHeight() + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            refresh();
            if (text.isEmpty()) {
                return;
            }
            int width = measureWidth(ctx);
            int height = measureHeight(ctx);
            ctx.fillRect(0, 0, width, height, theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh() {
            if (!showName.get()) {
                text = "";
                return;
            }
            String name = adapter.hoveredItemName();
            if (name.equals(lastName)) {
                return;
            }
            lastName = name;
            text = name.isEmpty() ? "" : name;
        }
    }
}
