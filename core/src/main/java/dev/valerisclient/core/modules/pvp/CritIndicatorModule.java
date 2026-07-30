package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Indicates when a critical hit is available (sword / mace). */
public final class CritIndicatorModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public CritIndicatorModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("crit-indicator", "Crit Indicator", "Shows when a critical hit is ready", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "crit-indicator", "Crit Indicator", themes, HudAnchor.BOTTOM_CENTER, 0, -52));
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
        boolean ready = !adapter.playerOnGround()
                && adapter.playerFallDistance() > 0.05f
                && adapter.attackCooldown() >= 0.95f;
        element.setText(ready ? "Crit: READY" : "Crit: —");
    }
}
