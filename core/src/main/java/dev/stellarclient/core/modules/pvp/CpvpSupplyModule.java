package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Combined crystal PvP hotbar supply — obsidian + crystals + totems. */
public final class CpvpSupplyModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public CpvpSupplyModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("cpvp-supply", "CPvP Supply", "Obsidian, crystals and totems at a glance", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "cpvp-supply", "CPvP Supply", themes, HudAnchor.TOP_LEFT, 4, 68));
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
        int obs = adapter.countHotbarItemsMatching("obsidian");
        int cry = adapter.countHotbarItemsMatching("crystal");
        int tot = adapter.countHotbarItemsMatching("totem");
        element.setText("CPvP O" + obs + " C" + cry + " T" + tot);
    }
}
