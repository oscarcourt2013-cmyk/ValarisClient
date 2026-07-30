package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;

/** Respawns immediately on death. */
public final class AutoRespawnModule extends Module {

    private final MinecraftAdapter adapter;

    public AutoRespawnModule(MinecraftAdapter adapter) {
        super("auto-respawn", "Auto Respawn", "Respawn automatically when you die", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> onTick());
    }

    private void onTick() {
        if (adapter.isDead()) {
            adapter.respawn();
        }
    }
}
