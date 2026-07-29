package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

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
