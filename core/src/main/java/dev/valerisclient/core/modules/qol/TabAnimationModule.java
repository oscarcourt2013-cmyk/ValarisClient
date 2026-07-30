package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.DoubleSetting;
import dev.valerisclient.core.module.IntSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.state.TabAnimationState;

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
