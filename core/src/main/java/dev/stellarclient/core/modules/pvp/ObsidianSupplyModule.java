package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Obsidian count in hotbar for crystal PvP. */
public final class ObsidianSupplyModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ObsidianSupplyModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("obsidian-supply", "Obsidian Supply", "Obsidian stacks ready in hotbar", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "obsidian-supply", "Obsidian Supply", themes, HudAnchor.TOP_LEFT, 4, 36));
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
        int count = adapter.countHotbarItemsMatching("obsidian");
        element.setText("Obsidian: " + count);
    }
}
