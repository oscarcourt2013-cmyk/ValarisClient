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

/** Shows configured price next to the held item name. */
public final class ItemPriceTagModule extends Module {

    private final StringSetting itemPrices = addSetting(new StringSetting(
            "item-prices", "Item prices", "Comma-separated item:price pairs", "diamond:100,iron_ingot:5"));

    private final Element element;
    private final MinecraftAdapter adapter;
    private String lastConfig = "";

    public ItemPriceTagModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("item-price-tag", "Item Price Tag", "Shows configured price for held item", ModuleCategory.QOL);
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
        String config = itemPrices.get();
        if (!config.equals(lastConfig)) {
            lastConfig = config;
        }
        SmpPriceTable table = SmpPriceTable.parse(config);
        String name = adapter.heldItemName();
        if (name.isEmpty()) {
            element.setText("Price: —");
            return;
        }
        double price = table.lookup(name);
        if (price <= 0) {
            element.setText(name + ": no price");
        } else {
            element.setText(name + ": " + ChestValueModule.formatMoney(price));
        }
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private String text = "Price: —";

        Element(ThemeManager themes) {
            super("item-price-tag", "Item Price Tag", HudAnchor.BOTTOM_RIGHT, -4, -52);
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
