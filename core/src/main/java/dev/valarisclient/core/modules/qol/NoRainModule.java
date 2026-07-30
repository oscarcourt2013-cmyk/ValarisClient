package dev.valarisclient.core.modules.qol;

import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.state.NoRainState;

/** Hides rain and snow particles on the client without changing server weather. */
public final class NoRainModule extends Module {

    public NoRainModule() {
        super("no-rain", "No Rain", "Hide rain and snow on your screen", ModuleCategory.QOL);
    }

    @Override
    protected void onEnable() {
        NoRainState.setActive(true);
    }

    @Override
    protected void onDisable() {
        NoRainState.setActive(false);
    }
}
