package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.state.LowFireState;

/** Lowers the on-fire screen overlay without changing burn damage or duration. */
public final class LowFireModule extends Module {

    private static final float MAX_TRANSLATE = 0.8F;

    private final DoubleSetting heightOffset =
            addSetting(new DoubleSetting(
                    "height",
                    "Height offset",
                    "How far to lower the fire overlay",
                    0.35,
                    0.0,
                    1.0));

    public LowFireModule() {
        super("low-fire", "Low Fire", "Lower the fire overlay while burning", ModuleCategory.QOL);
        listen(ClientTickEvent.class, event -> syncOffset());
    }

    @Override
    protected void onEnable() {
        LowFireState.setActive(true);
        syncOffset();
    }

    @Override
    protected void onDisable() {
        LowFireState.setActive(false);
    }

    private void syncOffset() {
        LowFireState.setHeightOffset((float) (heightOffset.get() * MAX_TRANSLATE));
    }
}
