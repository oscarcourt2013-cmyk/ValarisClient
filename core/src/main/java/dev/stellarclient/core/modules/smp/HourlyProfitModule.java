package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Shows profit per hour from a manual session profit counter. */
public final class HourlyProfitModule extends Module {

    private final DoubleSetting sessionProfit = addSetting(new DoubleSetting(
            "session-profit", "Session profit", "Profit earned this session", 0, 0, 1_000_000_000));

    private final SmpLineHud element;
    private long sessionStartMillis;

    public HourlyProfitModule(HudManager hud, ThemeManager themes) {
        super("hourly-profit", "Hourly Profit", "Estimated coins per hour this session", ModuleCategory.QOL);
        this.element = hud.register(new SmpLineHud(
                "hourly-profit", "Hourly Profit", themes, HudAnchor.TOP_LEFT, 4, 140));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> refresh());
    }

    @Override
    protected void onEnable() {
        sessionStartMillis = System.currentTimeMillis();
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void refresh() {
        long elapsed = Math.max(60_000L, System.currentTimeMillis() - sessionStartMillis);
        double hours = elapsed / 3_600_000.0;
        double rate = sessionProfit.get() / hours;
        element.setText("Rate: " + ChestValueModule.formatMoney(rate) + "/h");
    }
}
