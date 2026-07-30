package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

/** Chorus fruit cooldown for pearl-chorus escapes. */
public final class ChorusCooldownModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ChorusCooldownModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("chorus-cooldown", "Chorus Cooldown", "Chorus fruit teleport readiness", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "chorus-cooldown", "Chorus Cooldown", themes, HudAnchor.BOTTOM_LEFT, 4, -96));
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
        element.setText(PvpFormat.cooldown("Chorus", adapter.itemCooldownReady("chorus")));
    }
}
