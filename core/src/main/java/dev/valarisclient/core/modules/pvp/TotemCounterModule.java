package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

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
