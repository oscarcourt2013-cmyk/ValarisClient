package dev.valerisclient.core.modules.survival;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Y level with mining depth hints. */
public final class DepthHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public DepthHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("depth-hud", "Depth HUD", "Altitude and mining layer hints", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "depth-hud", "Depth HUD", themes, HudAnchor.BOTTOM_LEFT, 4, -16));
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
        int y = (int) Math.floor(adapter.playerY());
        element.setText("Y: " + y + SurvivalFormat.depthHint(y));
    }
}
