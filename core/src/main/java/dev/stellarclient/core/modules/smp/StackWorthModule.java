package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.ThemeManager;

/** Shows configured value of the held item stack. */
public final class StackWorthModule extends Module {

    private final StringSetting itemPrices = addSetting(new StringSetting(
            "item-prices", "Item prices", "Comma-separated item:price pairs", "diamond:100,iron_ingot:5"));

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public StackWorthModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("stack-worth", "Stack Worth", "Value of the full stack in your hand", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "stack-worth", "Stack Worth", themes, HudAnchor.BOTTOM_RIGHT, -4, -68));
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
        int count = adapter.heldItemCount();
        if (name.isEmpty() || count <= 0) {
            element.setText("Stack: empty");
            return;
        }
        double unit = SmpPriceTable.parse(itemPrices.get()).lookup(name);
        if (unit <= 0) {
            element.setText("Stack: no price");
            return;
        }
        element.setText("Stack: " + ChestValueModule.formatMoney(unit * count));
    }
}
