package dev.valarisclient.core.modules.smp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.module.StringSetting;
import dev.valarisclient.core.theme.ThemeManager;

/** Sums item prices for hotbar slots only (0–8). */
public final class HotbarValueModule extends Module {

    private static final int HOTBAR_SLOTS = 9;

    private final StringSetting itemPrices = addSetting(new StringSetting(
            "item-prices", "Item prices", "Comma-separated item:price pairs", "diamond:100,iron_ingot:5"));

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public HotbarValueModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("hotbar-value", "Hotbar Value", "Total sell value of your hotbar", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "hotbar-value", "Hotbar Value", themes, HudAnchor.TOP_LEFT, 4, 156));
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
            element.setText("Hotbar: set prices");
            return;
        }
        double total = 0;
        int limit = Math.min(HOTBAR_SLOTS, adapter.inventorySlotCount());
        for (int i = 0; i < limit; i++) {
            String name = adapter.inventorySlotItemName(i);
            if (name.isEmpty()) {
                continue;
            }
            double unit = table.lookup(name);
            if (unit > 0) {
                total += unit * adapter.inventorySlotItemCount(i);
            }
        }
        element.setText("Hotbar: " + ChestValueModule.formatMoney(total));
    }
}
