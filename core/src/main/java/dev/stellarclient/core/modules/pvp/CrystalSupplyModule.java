package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** End crystal count in hotbar for crystal PvP. */
public final class CrystalSupplyModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public CrystalSupplyModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("crystal-supply", "Crystal Supply", "End crystals ready in hotbar", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "crystal-supply", "Crystal Supply", themes, HudAnchor.TOP_LEFT, 4, 52));
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
        int count = adapter.countHotbarItemsMatching("crystal");
        element.setText("Crystals: " + count);
    }
}
