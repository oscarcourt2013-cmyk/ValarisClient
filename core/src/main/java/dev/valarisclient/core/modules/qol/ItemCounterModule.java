package dev.valarisclient.core.modules.qol;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/** HUD showing the held item stack count. */
public final class ItemCounterModule extends Module {

    private final Element element;

    public ItemCounterModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("item-counter", "Item Counter", "Shows your held item count", ModuleCategory.QOL);
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

        private int lastCount = -1;
        private String lastName = "";
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("item-counter", "Item Counter", HudAnchor.BOTTOM_RIGHT, -4, -20);
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
            refresh();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh() {
            String name = adapter.heldItemName();
            int count = adapter.heldItemCount();
            if (name.equals(lastName) && count == lastCount) {
                return;
            }
            lastName = name;
            lastCount = count;
            if (name.isEmpty()) {
                text = "Empty hand";
            } else {
                text = name + " x" + count;
            }
        }
    }
}
