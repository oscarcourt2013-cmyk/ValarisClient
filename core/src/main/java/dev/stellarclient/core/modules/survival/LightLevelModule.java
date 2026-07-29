package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Block light level â€” mob spawning and crop growth. */
public final class LightLevelModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public LightLevelModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("light-level", "Light Level", "Block light at your position", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "light-level", "Light Level", themes, HudAnchor.TOP_LEFT, 4, 36));
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
        int light = adapter.blockLightLevel();
        String tag = light <= 7 ? " (mobs)" : "";
        element.setText("Light: " + light + tag);
    }
}
