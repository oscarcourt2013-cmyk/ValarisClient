package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** In-game clock for planning travel, farming and sleeping. */
public final class DayTimeModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public DayTimeModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("day-time", "Day Time", "Minecraft time and day/night phase", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "day-time", "Day Time", themes, HudAnchor.TOP_LEFT, 4, 4));
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
        long time = adapter.worldDayTime();
        String phase = SurvivalFormat.isNight(time) ? "Night" : "Day";
        element.setText(phase + " " + SurvivalFormat.clock(time));
    }
}
