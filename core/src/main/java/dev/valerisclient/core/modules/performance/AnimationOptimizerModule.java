package dev.valerisclient.core.modules.performance;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;

/** Disables fancy graphics to reduce animation and effect overhead. */
public final class AnimationOptimizerModule extends Module {

    private final MinecraftAdapter adapter;
    private boolean savedFancyGraphics;

    public AnimationOptimizerModule(MinecraftAdapter adapter) {
        super("animation-optimizer", "Animation Optimizer", "Disables fancy graphics", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
    }

    @Override
    protected void onEnable() {
        savedFancyGraphics = adapter.fancyGraphics();
        adapter.setFancyGraphics(false);
    }

    @Override
    protected void onDisable() {
        adapter.setFancyGraphics(savedFancyGraphics);
    }
}
