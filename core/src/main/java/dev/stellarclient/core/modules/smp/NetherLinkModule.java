package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Converts coordinates between overworld and nether for portal travel. */
public final class NetherLinkModule extends Module {

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public NetherLinkModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("nether-link", "Nether Link", "Linked overworld/nether coordinates for travel", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "nether-link", "Nether Link", themes, HudAnchor.TOP_RIGHT, -4, 116));
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
        int x = (int) Math.floor(adapter.playerX());
        int z = (int) Math.floor(adapter.playerZ());
        String dim = adapter.dimensionId();
        if (dim.contains("nether")) {
            element.setText("OW: " + (x * 8) + ", " + (z * 8));
        } else {
            element.setText("Nether: " + (x / 8) + ", " + (z / 8));
        }
    }
}
