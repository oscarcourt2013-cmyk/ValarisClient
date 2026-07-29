package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Ender pearl cooldown readiness. */
public final class PearlCooldownModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public PearlCooldownModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("pearl-cooldown", "Pearl Cooldown", "Ender pearl throw readiness", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "pearl-cooldown", "Pearl Cooldown", themes, HudAnchor.BOTTOM_LEFT, 4, -48));
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
        element.setText(PvpFormat.cooldown("Pearl", adapter.itemCooldownReady("pearl")));
    }
}
