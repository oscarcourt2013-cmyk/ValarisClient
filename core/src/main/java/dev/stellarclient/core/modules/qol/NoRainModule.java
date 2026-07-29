package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.state.NoRainState;

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
