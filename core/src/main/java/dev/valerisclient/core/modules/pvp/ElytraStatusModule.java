package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.SimpleLineHud;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Elytra flight status for stasis and chase fights. */
public final class ElytraStatusModule extends Module {

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public ElytraStatusModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("elytra-status", "Elytra Status", "Shows when elytra flight is active", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "elytra-status", "Elytra Status", themes, HudAnchor.TOP_RIGHT, -4, 4));
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
        if (adapter.playerFallFlying()) {
            element.setText("Elytra: FLYING");
        } else {
            float fall = adapter.playerFallDistance();
            element.setText(fall > 0.1f ? "Elytra: falling " + String.format("%.1f", fall) : "Elytra: grounded");
        }
    }
}
