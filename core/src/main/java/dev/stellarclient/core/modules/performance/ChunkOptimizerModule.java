package dev.stellarclient.core.modules.performance;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

/** Lowers simulation distance to reduce chunk ticking load. */
public final class ChunkOptimizerModule extends Module {

    private static final int TARGET_SIMULATION_DISTANCE = 5;

    private final MinecraftAdapter adapter;
    private int savedSimulationDistance = -1;

    public ChunkOptimizerModule(MinecraftAdapter adapter) {
        super("chunk-optimizer", "Chunk Optimizer", "Reduces simulation distance", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        savedSimulationDistance = adapter.simulationDistance();
        adapter.setSimulationDistance(Math.min(savedSimulationDistance, TARGET_SIMULATION_DISTANCE));
    }

    @Override
    protected void onDisable() {
        if (savedSimulationDistance >= 0) {
            adapter.setSimulationDistance(savedSimulationDistance);
            savedSimulationDistance = -1;
        }
    }
}
