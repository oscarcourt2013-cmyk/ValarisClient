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

/** Estimates chest value from configured item prices when a container is open. */
public final class ChestValueModule extends Module {

    private final StringSetting itemPrices = addSetting(new StringSetting(
            "item-prices", "Item prices", "Comma-separated item:price pairs", "diamond:100,iron_ingot:5"));

    private final Element element;
    private final MinecraftAdapter adapter;
    private String lastPriceConfig = "";

    public ChestValueModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("chest-value", "Chest Value", "Sums manual item prices in open containers", ModuleCategory.QOL);
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
        if (!config.equals(lastPriceConfig)) {
            lastPriceConfig = config;
        }
        SmpPriceTable table = SmpPriceTable.parse(config);
        if (!adapter.isContainerScreenOpen() || table.isEmpty()) {
            element.setText(adapter.isContainerScreenOpen() ? "Chest: set prices" : "Chest: closed");
            return;
        }
        double total = 0;
        int priced = 0;
        for (int i = 0; i < adapter.openContainerStorageSlotCount(); i++) {
            String name = adapter.openContainerSlotItemName(i);
            if (name.isEmpty()) {
                continue;
            }
            double unit = table.lookup(name);
            if (unit > 0) {
                total += unit * adapter.openContainerSlotItemCount(i);
                priced++;
            }
        }
        if (priced == 0) {
            element.setText("Chest: no priced items");
        } else {
            element.setText("Chest: " + formatMoney(total));
        }
    }

    static String formatMoney(double value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000);
        }
        if (value >= 1_000) {
            return String.format("%.1fK", value / 1_000);
        }
        if (value == Math.floor(value)) {
            return String.format("%.0f", value);
        }
        return String.format("%.2f", value);
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private String text = "Chest: closed";

        Element(ThemeManager themes) {
            super("chest-value", "Chest Value", HudAnchor.TOP_LEFT, 4, 76);
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
