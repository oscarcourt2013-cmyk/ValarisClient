package dev.stellarclient.core.modules.performance;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

/** Lowers render load by trimming distance, clouds, and particles. */
public final class FpsBoosterModule extends Module {

    private static final int TARGET_RENDER_DISTANCE = 6;
    private static final int MINIMAL_PARTICLES = 2;

    private final MinecraftAdapter adapter;

    private int savedRenderDistance = -1;
    private boolean savedCloudsEnabled;
    private int savedParticleSetting = -1;

    public FpsBoosterModule(MinecraftAdapter adapter) {
        super("fps-booster", "FPS Booster", "Lowers render distance, clouds, and particles", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        savedRenderDistance = adapter.renderDistance();
        savedCloudsEnabled = adapter.cloudsEnabled();
        savedParticleSetting = adapter.particleSetting();

        adapter.setRenderDistance(Math.min(savedRenderDistance, TARGET_RENDER_DISTANCE));
        adapter.setCloudsEnabled(false);
        adapter.setParticleSetting(MINIMAL_PARTICLES);
    }

    @Override
    protected void onDisable() {
        if (savedRenderDistance >= 0) {
            adapter.setRenderDistance(savedRenderDistance);
            savedRenderDistance = -1;
        }
        adapter.setCloudsEnabled(savedCloudsEnabled);
        if (savedParticleSetting >= 0) {
            adapter.setParticleSetting(savedParticleSetting);
            savedParticleSetting = -1;
        }
    }
}
