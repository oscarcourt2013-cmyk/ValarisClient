package dev.valerisclient.core.modules.smp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.DoubleSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Estimates walk time to a configured destination. */
public final class TravelEtaModule extends Module {

    private static final double WALK_BLOCKS_PER_SECOND = 4.3;

    private final DoubleSetting targetX = addSetting(new DoubleSetting(
            "target-x", "Target X", "Destination X coordinate", 0, -30_000_000, 30_000_000));
    private final DoubleSetting targetZ = addSetting(new DoubleSetting(
            "target-z", "Target Z", "Destination Z coordinate", 0, -30_000_000, 30_000_000));

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public TravelEtaModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("travel-eta", "Travel ETA", "Walk time estimate to a shop or base", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "travel-eta", "Travel ETA", themes, HudAnchor.TOP_CENTER, 0, 52));
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
        double dx = targetX.get() - adapter.playerX();
        double dz = targetZ.get() - adapter.playerZ();
        double blocks = Math.sqrt(dx * dx + dz * dz);
        int seconds = (int) Math.ceil(blocks / WALK_BLOCKS_PER_SECOND);
        int min = seconds / 60;
        int sec = seconds % 60;
        element.setText("ETA: " + min + "m " + sec + "s (" + (int) blocks + "m)");
    }
}
