package dev.valarisclient.core.modules.smp;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.DoubleSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

/** Tracks progress toward a manual money goal. */
public final class MoneyGoalModule extends Module {

    private final DoubleSetting goal = addSetting(new DoubleSetting(
            "goal", "Goal", "Target balance to reach", 100_000, 1, 1_000_000_000));
    private final DoubleSetting current = addSetting(new DoubleSetting(
            "current", "Current", "Your current balance (update manually)", 0, 0, 1_000_000_000));

    private final SmpLineHud element;

    public MoneyGoalModule(HudManager hud, ThemeManager themes) {
        super("money-goal", "Money Goal", "Progress bar toward a savings or buy goal", ModuleCategory.QOL);
        this.element = hud.register(new SmpLineHud(
                "money-goal", "Money Goal", themes, HudAnchor.TOP_LEFT, 4, 124));
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
        double target = Math.max(1, goal.get());
        double now = Math.max(0, current.get());
        int percent = (int) Math.min(100, now * 100 / target);
        element.setText("Goal: " + ChestValueModule.formatMoney(now) + " / "
                + ChestValueModule.formatMoney(target) + " (" + percent + "%)");
    }
}
