package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Shows XP level and progress â€” useful on economy servers with XP shops. */
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
