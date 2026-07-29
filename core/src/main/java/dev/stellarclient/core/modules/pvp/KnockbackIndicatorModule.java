package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Knockback potential from crit, sprint, and fall distance. */
public final class KnockbackIndicatorModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public KnockbackIndicatorModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("knockback-indicator", "Knockback Indicator", "Shows knockback potential", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "knockback-indicator", "Knockback Indicator", themes, HudAnchor.BOTTOM_CENTER, 0, -64));
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
        float score = 0f;
        if (adapter.isSprinting()) {
            score += 0.35f;
        }
        if (!adapter.playerOnGround() && adapter.playerFallDistance() > 0.05f) {
            score += 0.35f;
        }
        if (adapter.attackCooldown() >= 0.95f) {
            score += 0.2f;
        }
        score += Math.min(adapter.playerFallDistance() / 10f, 0.3f);
        String label = score >= 0.75f ? "High" : score >= 0.45f ? "Medium" : "Low";
        element.setText("KB: " + label + " (" + Math.round(score * 100) + "%)");
    }
}
