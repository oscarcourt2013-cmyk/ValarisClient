package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

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
        element.setText(ready ? "Crit: READY" : "Crit: â€”");
    }
}
