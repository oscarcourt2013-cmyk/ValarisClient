package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

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
