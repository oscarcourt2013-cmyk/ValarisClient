package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Fall distance and smash tier for mace PvP. */
public final class MaceSmashModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public MaceSmashModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("mace-smash", "Mace Smash", "Fall distance and smash damage tier", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "mace-smash", "Mace Smash", themes, HudAnchor.BOTTOM_CENTER, 0, -36));
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
        float fall = adapter.playerFallDistance();
        element.setText("Smash: " + String.format("%.1f", fall) + " " + PvpFormat.maceTier(fall));
    }
}
