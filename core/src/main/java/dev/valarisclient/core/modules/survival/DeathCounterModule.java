package dev.valarisclient.core.modules.survival;

import dev.valarisclient.core.event.PlayerDeathEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.SimpleLineHud;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

/** Deaths this session — hardcore and SMP tracking. */
public final class DeathCounterModule extends Module {

    private final SimpleLineHud element;
    private int deaths;

    public DeathCounterModule(HudManager hud, ThemeManager themes) {
        super("death-counter", "Death Counter", "Deaths since module was enabled", ModuleCategory.SURVIVAL);
        this.element = hud.register(new SimpleLineHud(
                "death-counter", "Death Counter", themes, HudAnchor.TOP_RIGHT, -4, 36));
        element.setVisible(false);
        listen(PlayerDeathEvent.class, event -> {
            deaths++;
            element.setText("Deaths: " + deaths);
        });
    }

    @Override
    protected void onEnable() {
        deaths = 0;
        element.setVisible(true);
        element.setText("Deaths: 0");
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }
}
