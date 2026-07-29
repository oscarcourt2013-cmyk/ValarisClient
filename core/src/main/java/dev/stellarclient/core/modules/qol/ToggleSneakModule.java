package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;

/** Keeps sneaking while enabled. */
public final class ToggleSneakModule extends Module {

    private final MinecraftAdapter adapter;

    public ToggleSneakModule(MinecraftAdapter adapter) {
        super("toggle-sneak", "Toggle Sneak", "Stay sneaking while enabled", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> onTick());
    }

    private void onTick() {
        adapter.setSneaking(true);
    }
}
