package dev.valerisclient.core.modules.smp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.IntSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.theme.ThemeManager;

/** Shows sell price after a configurable server tax percentage. */
public final class TaxCalculatorModule extends Module {

    private final StringSetting itemPrices = addSetting(new StringSetting(
            "item-prices", "Item prices", "Comma-separated item:price pairs", "diamond:100,iron_ingot:5"));
    private final IntSetting taxPercent = addSetting(new IntSetting(
            "tax-percent", "Tax %", "Server tax deducted from sell price", 10, 0, 50));

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public TaxCalculatorModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("tax-calculator", "Tax Calculator", "Net sell price after server tax", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "tax-calculator", "Tax Calculator", themes, HudAnchor.BOTTOM_RIGHT, -4, -100));
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
        String name = adapter.heldItemName();
        if (name.isEmpty()) {
            element.setText("Tax: empty hand");
            return;
        }
        double gross = SmpPriceTable.parse(itemPrices.get()).lookup(name);
        if (gross <= 0) {
            element.setText("Tax: no price");
            return;
        }
        double net = gross * (100 - taxPercent.get()) / 100.0;
        element.setText("Tax: " + ChestValueModule.formatMoney(net) + " net");
    }
}
