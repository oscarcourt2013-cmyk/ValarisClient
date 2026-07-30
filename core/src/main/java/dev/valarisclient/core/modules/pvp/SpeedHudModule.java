package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

/** Horizontal player speed in metres per second. */
public final class SpeedHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;
    private String lastText = "";

    public SpeedHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("speed-hud", "Speed HUD", "Shows your horizontal speed in m/s", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "speed-hud", "Speed HUD", themes, HudAnchor.TOP_LEFT, 4, 100));
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
        double speed = adapter.playerHorizontalSpeed();
        String text = String.format(java.util.Locale.ROOT, "%.1f m/s", speed);
        if (!text.equals(lastText)) {
            lastText = text;
            element.setText(text);
        }
    }
}
