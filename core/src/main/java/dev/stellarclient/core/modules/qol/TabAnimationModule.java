package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.state.TabAnimationState;

/** Lightweight fade/slide when opening the player list (TAB). */
public final class TabAnimationModule extends Module {

    private final IntSetting durationMs = addSetting(new IntSetting(
            "duration", "Duration", "Open animation length in ms", 160, 40, 400));
    private final DoubleSetting slide = addSetting(new DoubleSetting(
            "slide", "Slide", "Pixels to slide in from above", 6.0, 0.0, 16.0));

    public TabAnimationModule() {
        super("tab-animation", "TAB Animation",
                "Subtle fade and slide when opening the tab list", ModuleCategory.QOL);
        listen(ClientTickEvent.class, event -> sync());
    }

    @Override
    protected void onEnable() {
        TabAnimationState.setActive(true);
        sync();
    }

    @Override
    protected void onDisable() {
        TabAnimationState.reset();
    }

    private void sync() {
        TabAnimationState.setDurationMs(durationMs.get());
        TabAnimationState.setSlidePixels((float) slide.get());
    }
}
