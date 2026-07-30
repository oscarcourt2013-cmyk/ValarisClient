package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.theme.ThemeManager;

/** Shows the active inventory search term while a screen is open. */
public final class InventorySearchModule extends Module {

    private final StringSetting term =
            addSetting(new StringSetting("term", "Search", "Item name to find", ""));

    private final Element element;

    public InventorySearchModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("inventory-search", "Inventory Search", "Highlights items matching your search term", ModuleCategory.QOL);
        this.element = hud.register(new Element(themes, adapter, term));
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
        private final StringSetting term;

        private String lastTerm = "";
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter, StringSetting term) {
            super("inventory-search", "Inventory Search", HudAnchor.TOP_CENTER, 0, 20);
            this.themes = themes;
            this.adapter = adapter;
            this.term = term;
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
            if (!adapter.isScreenOpen()) {
                text = "";
                return;
            }
            String current = term.get();
            if (current.equals(lastTerm)) {
                return;
            }
            lastTerm = current;
            text = current.isBlank() ? "" : "Search: " + current;
        }
    }
}
