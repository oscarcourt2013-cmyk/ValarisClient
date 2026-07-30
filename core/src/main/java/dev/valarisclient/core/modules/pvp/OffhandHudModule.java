package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

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
