package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

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
