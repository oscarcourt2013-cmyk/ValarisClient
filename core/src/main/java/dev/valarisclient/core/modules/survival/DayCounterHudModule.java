package dev.valarisclient.core.modules.survival;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

/** Minecraft world day counter (day 0 = first day). */
public final class DayCounterHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;
    private long lastDay = Long.MIN_VALUE;

    public DayCounterHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("day-counter-hud", "Day Counter HUD", "Shows the current Minecraft world day number",
                ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "day-counter-hud", "Day Counter", themes, HudAnchor.TOP_LEFT, 4, 84));
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
        long day = adapter.worldDayNumber();
        if (day != lastDay) {
            lastDay = day;
            element.setText("Day " + day);
        }
    }
}
