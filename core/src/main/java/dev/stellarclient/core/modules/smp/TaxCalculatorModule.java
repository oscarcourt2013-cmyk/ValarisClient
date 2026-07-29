package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.ThemeManager;

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
