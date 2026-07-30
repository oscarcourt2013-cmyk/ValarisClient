package dev.valarisclient.core.modules.smp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.ThemeManager;

/** Shows XP level and progress — useful on economy servers with XP shops. */
public final class XpLevelHudModule extends Module {

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public XpLevelHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("xp-level-hud", "XP Level HUD", "Shows your XP level and bar progress", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "xp-level-hud", "XP Level HUD", themes, HudAnchor.TOP_RIGHT, -4, 100));
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
        int level = adapter.playerXpLevel();
        int percent = (int) (adapter.playerXpProgress() * 100);
        element.setText("XP: Lv " + level + " (" + percent + "%)");
    }
}
