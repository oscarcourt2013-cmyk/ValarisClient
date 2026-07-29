package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Totem of undying count in inventory. */
public final class TotemCounterModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public TotemCounterModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("totem-counter", "Totem Counter", "How many totems you are carrying", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "totem-counter", "Totem Counter", themes, HudAnchor.TOP_LEFT, 4, 4));
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
        int count = adapter.countItemsMatching("totem");
        element.setText("Totems: " + count);
    }
}
