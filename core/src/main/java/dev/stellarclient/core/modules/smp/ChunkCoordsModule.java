package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

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
