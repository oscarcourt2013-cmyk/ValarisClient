package dev.valerisclient.core.modules.smp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.theme.ThemeManager;

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
