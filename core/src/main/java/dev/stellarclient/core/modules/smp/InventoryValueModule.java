package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Estimates total inventory value from configured item prices. */
public final class InventoryValueModule extends Module {

    private final StringSetting itemPrices = addSetting(new StringSetting(
            "item-prices", "Item prices", "Comma-separated item:price pairs", "diamond:100,iron_ingot:5"));

    private final Element element;
    private final MinecraftAdapter adapter;

    public InventoryValueModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("inventory-value", "Inventory Value", "Sums manual item prices in your inventory", ModuleCategory.QOL);
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
        SmpPriceTable table = SmpPriceTable.parse(itemPrices.get());
        if (table.isEmpty()) {
            element.setText("Inv: set prices");
            return;
        }
        double total = 0;
        int priced = 0;
        for (int i = 0; i < adapter.inventorySlotCount(); i++) {
            String name = adapter.inventorySlotItemName(i);
            if (name.isEmpty()) {
                continue;
            }
            double unit = table.lookup(name);
            if (unit > 0) {
                total += unit * adapter.inventorySlotItemCount(i);
                priced++;
            }
        }
        if (priced == 0) {
            element.setText("Inv: no priced items");
        } else {
            element.setText("Inv: " + ChestValueModule.formatMoney(total));
        }
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private String text = "Inv: â€”";

        Element(ThemeManager themes) {
            super("inventory-value", "Inventory Value", HudAnchor.TOP_LEFT, 4, 108);
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
