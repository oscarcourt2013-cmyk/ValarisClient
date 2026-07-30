package dev.valarisclient.core.modules.qol;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;

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
