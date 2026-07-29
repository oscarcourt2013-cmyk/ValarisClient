package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.ThemeManager;

/** Estimates value of items inside a held shulker box. */
public final class ShulkerValueModule extends Module {

    private final StringSetting itemPrices = addSetting(new StringSetting(
            "item-prices", "Item prices", "Comma-separated item:price pairs", "diamond:100,iron_ingot:5"));

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public ShulkerValueModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("shulker-value", "Shulker Value", "Total value inside a held shulker box", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "shulker-value", "Shulker Value", themes, HudAnchor.BOTTOM_RIGHT, -4, -84));
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
        if (!adapter.hoveredItemIsShulkerBox() && !adapter.heldItemName().toLowerCase().contains("shulker")) {
            element.setText("Shulker: not holding");
            return;
        }
        SmpPriceTable table = SmpPriceTable.parse(itemPrices.get());
        if (table.isEmpty()) {
            element.setText("Shulker: set prices");
            return;
        }
        double total = 0;
        int slots = adapter.shulkerSlotCount();
        for (int i = 0; i < slots; i++) {
            String name = adapter.shulkerSlotItem(i);
            if (name.isEmpty()) {
                continue;
            }
            double unit = table.lookup(name);
            if (unit > 0) {
                total += unit * adapter.shulkerSlotCount(i);
            }
        }
        element.setText("Shulker: " + ChestValueModule.formatMoney(total));
    }
}
