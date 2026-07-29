package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Offhand shield durability for sword and axe fights. */
public final class ShieldDurabilityModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ShieldDurabilityModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("shield-durability", "Shield Durability", "Durability of shield in offhand", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "shield-durability", "Shield Durability", themes, HudAnchor.BOTTOM_RIGHT, -4, -64));
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
        int percent = adapter.offhandShieldDurabilityPercent();
        if (percent < 0) {
            element.setText("Shield: none");
        } else {
            element.setText("Shield: " + percent + "%");
        }
    }
}
