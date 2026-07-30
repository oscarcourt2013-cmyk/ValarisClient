package dev.valarisclient.core.modules.qol;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.DoubleSetting;
import dev.valarisclient.core.module.IntSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.state.TabAnimationState;

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
