package dev.valerisclient.core.modules.survival;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

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
