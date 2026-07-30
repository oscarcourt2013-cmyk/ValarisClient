package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Shows current offhand item — totem, shield, gapple, etc. */
public final class OffhandHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public OffhandHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("offhand-hud", "Offhand HUD", "Item currently in your offhand", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "offhand-hud", "Offhand HUD", themes, HudAnchor.BOTTOM_RIGHT, -4, -80));
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
        String offhand = adapter.offhandItemName();
        element.setText(offhand.isEmpty() ? "Offhand: empty" : "Offhand: " + offhand);
    }
}
