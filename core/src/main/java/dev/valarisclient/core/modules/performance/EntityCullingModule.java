package dev.valarisclient.core.modules.performance;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;

/** Reduces how far away entities are rendered. */
public final class EntityCullingModule extends Module {

    private static final int CULLED_ENTITY_DISTANCE = 50;

    private final MinecraftAdapter adapter;
    private int savedEntityDistance = -1;

    public EntityCullingModule(MinecraftAdapter adapter) {
        super("entity-culling", "Entity Culling", "Lowers entity render distance", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        savedEntityDistance = adapter.entityDistance();
        adapter.setEntityDistance(CULLED_ENTITY_DISTANCE);
    }

    @Override
    protected void onDisable() {
        if (savedEntityDistance >= 0) {
            adapter.setEntityDistance(savedEntityDistance);
            savedEntityDistance = -1;
        }
    }
}
