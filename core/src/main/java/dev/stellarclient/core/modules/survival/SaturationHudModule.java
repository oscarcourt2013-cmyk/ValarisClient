package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Hunger saturation for sprint and regen planning. */
public final class SaturationHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public SaturationHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("saturation-hud", "Saturation HUD", "Food saturation level", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "saturation-hud", "Saturation HUD", themes, HudAnchor.TOP_LEFT, 4, 52));
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
        element.setText(String.format("Sat: %.1f", adapter.playerSaturation()));
    }
}
