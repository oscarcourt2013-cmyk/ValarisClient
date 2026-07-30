package dev.valerisclient.core.modules.survival;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Distance to world spawn. */
public final class SpawnDistanceModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public SpawnDistanceModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("spawn-distance", "Spawn Distance", "Blocks from world spawn", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "spawn-distance", "Spawn Distance", themes, HudAnchor.TOP_RIGHT, -4, 52));
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
        element.setText("Spawn: " + (int) adapter.spawnDistance() + "m");
    }
}
