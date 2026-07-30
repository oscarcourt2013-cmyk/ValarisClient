package dev.valerisclient.core.modules.performance;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;

/** Caps framerate while the game window is unfocused. */
public final class DynamicFpsModule extends Module {

    private static final int UNFOCUSED_MAX_FPS = 30;

    private final MinecraftAdapter adapter;

    private int savedMaxFps = -1;
    private boolean capped;

    public DynamicFpsModule(MinecraftAdapter adapter) {
        super("dynamic-fps", "Dynamic FPS", "Lowers max FPS when unfocused", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
        listen(ClientTickEvent.class, this::onTick);
    }

    @Override
    protected void onDisable() {
        restoreFps();
    }

    private void onTick(ClientTickEvent event) {
        if (adapter.isWindowFocused()) {
            restoreFps();
        } else if (!capped) {
            savedMaxFps = adapter.maxFps();
            adapter.setMaxFps(UNFOCUSED_MAX_FPS);
            capped = true;
        }
    }

    private void restoreFps() {
        if (capped && savedMaxFps >= 0) {
            adapter.setMaxFps(savedMaxFps);
            savedMaxFps = -1;
            capped = false;
        }
    }
}
