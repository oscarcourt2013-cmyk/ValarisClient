package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

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
