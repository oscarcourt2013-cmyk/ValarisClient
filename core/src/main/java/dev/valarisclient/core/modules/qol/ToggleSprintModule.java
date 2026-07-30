package dev.valarisclient.core.modules.qol;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;

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
