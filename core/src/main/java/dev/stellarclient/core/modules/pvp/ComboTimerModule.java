package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.AttackEntityEvent;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.event.PlayerDamageEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Time until combo resets (~2s decay window). */
public final class ComboTimerModule extends Module {

    private static final long COMBO_WINDOW_MS = 2000L;

    private final SimpleLineHud element;
    private long lastHitMillis;
    private boolean comboActive;

    public ComboTimerModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("combo-timer", "Combo Timer", "Shows time until combo resets", ModuleCategory.PVP);
        this.element = hud.register(new SimpleLineHud(
                "combo-timer", "Combo Timer", themes, HudAnchor.TOP_RIGHT, -4, 64));
        element.setVisible(false);
        listen(AttackEntityEvent.class, event -> {
            lastHitMillis = System.currentTimeMillis();
            comboActive = true;
        });
        listen(PlayerDamageEvent.class, event -> comboActive = false);
        listen(ClientTickEvent.class, event -> refresh());
    }

    @Override
    protected void onEnable() {
        comboActive = false;
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void refresh() {
        if (!comboActive) {
            element.setText("Combo: —");
            return;
        }
        long elapsed = System.currentTimeMillis() - lastHitMillis;
        long remaining = COMBO_WINDOW_MS - elapsed;
        if (remaining <= 0) {
            comboActive = false;
            element.setText("Combo: reset");
            return;
        }
        element.setText(String.format("Combo: %.1fs", remaining / 1000.0));
    }
}
