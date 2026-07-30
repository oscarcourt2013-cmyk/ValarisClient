package dev.valerisclient.core.modules.creator;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;

/** Hides the in-game HUD for clean screenshots. */
public final class ScreenshotModeModule extends Module {

    private final MinecraftAdapter adapter;
    private boolean hudWasHidden;

    public ScreenshotModeModule(MinecraftAdapter adapter) {
        super("screenshot-mode", "Screenshot Mode", "Hides the HUD for clean screenshots", ModuleCategory.CREATOR);
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        hudWasHidden = adapter.isHudHidden();
        adapter.setHudHidden(true);
    }

    @Override
    protected void onDisable() {
        adapter.setHudHidden(hudWasHidden);
    }
}
