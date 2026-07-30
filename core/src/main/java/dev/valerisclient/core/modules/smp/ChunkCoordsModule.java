package dev.valerisclient.core.modules.smp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.ThemeManager;

/** Shows chunk coordinates and distance to chunk border (claiming/building). */
public final class ChunkCoordsModule extends Module {

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public ChunkCoordsModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("chunk-coords", "Chunk Coords", "Chunk position and blocks to nearest border", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "chunk-coords", "Chunk Coords", themes, HudAnchor.TOP_LEFT, 4, 172));
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
        int bx = (int) Math.floor(adapter.playerX());
        int bz = (int) Math.floor(adapter.playerZ());
        int chunkX = Math.floorDiv(bx, 16);
        int chunkZ = Math.floorDiv(bz, 16);
        int edgeX = Math.min(bx & 15, 15 - (bx & 15));
        int edgeZ = Math.min(bz & 15, 15 - (bz & 15));
        int edge = Math.min(edgeX, edgeZ);
        element.setText("Chunk: " + chunkX + ", " + chunkZ + " | edge " + edge + "b");
    }
}
