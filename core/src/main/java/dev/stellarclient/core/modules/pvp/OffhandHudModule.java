package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Shows current offhand item â€” totem, shield, gapple, etc. */
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
