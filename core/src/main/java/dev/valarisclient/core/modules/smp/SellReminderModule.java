package dev.valarisclient.core.modules.smp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.module.StringSetting;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/** Shows held item with an optional manual sell price note. */
public final class SellReminderModule extends Module {

    private final StringSetting sellPrice = addSetting(new StringSetting(
            "sell-price", "Sell price", "Manual sell price note for the held item", ""));

    private final Element element;

    public SellReminderModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("sell-reminder", "Sell Reminder", "Shows held item and your sell price note", ModuleCategory.QOL);
        this.element = hud.register(new Element(themes, adapter, sellPrice));
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
        private final StringSetting sellPrice;

        private String lastName = "";
        private int lastCount = -1;
        private String lastPrice = "";
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter, StringSetting sellPrice) {
            super("sell-reminder", "Sell Reminder", HudAnchor.BOTTOM_RIGHT, -4, -36);
            this.themes = themes;
            this.adapter = adapter;
            this.sellPrice = sellPrice;
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
            String price = sellPrice.get();
            if (name.equals(lastName) && count == lastCount && price.equals(lastPrice)) {
                return;
            }
            lastName = name;
            lastCount = count;
            lastPrice = price;
            if (name.isEmpty()) {
                text = "Sell: empty hand";
            } else if (price.isBlank()) {
                text = "Sell: " + name + " x" + count;
            } else {
                text = "Sell: " + name + " x" + count + " @ " + price;
            }
        }
    }
}
