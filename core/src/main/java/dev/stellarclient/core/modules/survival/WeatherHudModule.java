package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Current weather â€” useful before travel or mob farms. */
public final class WeatherHudModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public WeatherHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("weather-hud", "Weather HUD", "Rain and thunder status", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "weather-hud", "Weather HUD", themes, HudAnchor.TOP_LEFT, 4, 20));
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
        if (adapter.worldThundering()) {
            element.setText("Weather: Storm");
        } else if (adapter.worldRaining()) {
            element.setText("Weather: Rain");
        } else {
            element.setText("Weather: Clear");
        }
    }
}
