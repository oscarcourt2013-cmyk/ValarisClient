package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.state.AlwaysDayState;

/** Forces a bright daytime sky on the client without changing server time. */
public final class AlwaysDayModule extends Module {

    public AlwaysDayModule() {
        super("always-day", "Always Day", "Render the world as daytime on your screen", ModuleCategory.QOL);
    }

    @Override
    protected void onEnable() {
        AlwaysDayState.setActive(true);
    }

    @Override
    protected void onDisable() {
        AlwaysDayState.setActive(false);
    }
}
