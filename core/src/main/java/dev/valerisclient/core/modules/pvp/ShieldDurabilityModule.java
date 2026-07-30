package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

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
