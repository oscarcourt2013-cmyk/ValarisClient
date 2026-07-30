package dev.valarisclient.core.modules.survival;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

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
