package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Distance in blocks to crosshair target — reach check for sword PvP. */
public final class ReachHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ReachHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("reach-hud", "Reach HUD", "Distance to your crosshair target", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "reach-hud", "Reach HUD", themes, HudAnchor.TOP_CENTER, 0, 20));
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
        if (!adapter.hasTarget()) {
            element.setText("Reach: —");
            return;
        }
        element.setText("Reach: " + String.format("%.1f", adapter.targetDistance()) + "m");
    }
}
