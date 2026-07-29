package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.SimpleLineHud;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

import java.util.Locale;

/** Predicted ender pearl landing from look raycast. */
public final class PearlLandingMarkerModule extends Module {

    private static final double MAX_RANGE = 48.0;

    private final SimpleLineHud element;
    private final MinecraftAdapter adapter;

    public PearlLandingMarkerModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("pearl-landing-marker", "Pearl Landing Marker", "Shows predicted pearl landing", ModuleCategory.PVP);
        this.adapter = adapter;
        this.element = hud.register(new SimpleLineHud(
                "pearl-landing-marker", "Pearl Landing", themes, HudAnchor.BOTTOM_LEFT, 4, -20));
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
        String held = adapter.heldItemName().toLowerCase(Locale.ROOT);
        String off = adapter.offhandItemName().toLowerCase(Locale.ROOT);
        if (!held.contains("ender pearl") && !off.contains("ender pearl")) {
            element.setText("Pearl: â€”");
            return;
        }
        double[] hit = adapter.raycastLookBlock(MAX_RANGE);
        if (hit == null) {
            element.setText("Pearl: no block");
            return;
        }
        element.setText(String.format(Locale.ROOT,
                "Pearl â†’ %.0f, %.0f, %.0f (%.1fm)",
                hit[0], hit[1], hit[2], hit[3]));
    }
}
