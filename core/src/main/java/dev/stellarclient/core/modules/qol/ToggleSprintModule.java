package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

/** Keeps sprinting while moving forward. */
public final class ToggleSprintModule extends Module {

    private final MinecraftAdapter adapter;

    public ToggleSprintModule(MinecraftAdapter adapter) {
        super("toggle-sprint", "Toggle Sprint", "Sprint automatically while moving forward", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> onTick());
    }

    private void onTick() {
        if (adapter.isMovingForward()) {
            adapter.setSprinting(true);
        }
    }
}
