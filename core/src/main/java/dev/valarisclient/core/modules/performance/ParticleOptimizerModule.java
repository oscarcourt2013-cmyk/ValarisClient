package dev.valarisclient.core.modules.performance;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;

/** Forces particles to the minimal setting while enabled. */
public final class ParticleOptimizerModule extends Module {

    private static final int MINIMAL_PARTICLES = 2;

    private final MinecraftAdapter adapter;
    private int savedParticleSetting = -1;

    public ParticleOptimizerModule(MinecraftAdapter adapter) {
        super("particle-optimizer", "Particle Optimizer", "Sets particles to minimal", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        savedParticleSetting = adapter.particleSetting();
        adapter.setParticleSetting(MINIMAL_PARTICLES);
    }

    @Override
    protected void onDisable() {
        if (savedParticleSetting >= 0) {
            adapter.setParticleSetting(savedParticleSetting);
            savedParticleSetting = -1;
        }
    }
}
