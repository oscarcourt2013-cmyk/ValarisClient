package dev.stellarclient.core.modules.performance;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

/** Slightly raises render distance and surfaces fast-loading tips. */
public final class FastLoadingModule extends Module {

    private static final int RENDER_DISTANCE_BOOST = 2;
    private static final int MAX_RENDER_DISTANCE = 32;

    private final MinecraftAdapter adapter;
    private int savedRenderDistance = -1;

    public FastLoadingModule(MinecraftAdapter adapter) {
        super("fast-loading", "Fast Loading", "Boosts render distance for quicker chunk loading", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        savedRenderDistance = adapter.renderDistance();
        int boosted = Math.min(savedRenderDistance + RENDER_DISTANCE_BOOST, MAX_RENDER_DISTANCE);
        adapter.setRenderDistance(boosted);
        adapter.sendChat("Fast loading tips enabled â€” stay near spawn while chunks generate.");
    }

    @Override
    protected void onDisable() {
        if (savedRenderDistance >= 0) {
            adapter.setRenderDistance(savedRenderDistance);
            savedRenderDistance = -1;
        }
    }
}
