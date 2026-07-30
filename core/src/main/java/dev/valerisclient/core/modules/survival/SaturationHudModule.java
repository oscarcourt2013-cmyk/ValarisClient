package dev.valerisclient.core.modules.survival;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

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
