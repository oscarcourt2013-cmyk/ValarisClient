package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;

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
