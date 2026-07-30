package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.state.NoRainState;

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
