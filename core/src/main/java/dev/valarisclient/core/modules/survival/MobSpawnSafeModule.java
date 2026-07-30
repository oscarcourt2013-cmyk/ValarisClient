package dev.valarisclient.core.modules.survival;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

/** Green/red indicator for mob spawn safety at feet. */
public final class MobSpawnSafeModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public MobSpawnSafeModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("mob-spawn-safe", "Mob Spawn Safe", "Shows if mobs can spawn here", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "mob-spawn-safe", "Mob Spawn Safe", themes, HudAnchor.TOP_LEFT, 4, 60));
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
        boolean safe = adapter.mobSpawnSafeAtFeet();
        int light = adapter.blockLightLevel();
        element.setText(safe ? "Spawn: SAFE (L" + light + ")" : "Spawn: UNSAFE (L" + light + ")");
    }
}
