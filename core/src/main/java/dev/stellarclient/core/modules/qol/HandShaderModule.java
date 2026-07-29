package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.ColorSetting;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.state.HandShaderState;

/** Simple first-person hand / held-item tint. */
public final class HandShaderModule extends Module {

    private final ColorSetting tint = addSetting(new ColorSetting(
            "tint", "Tint", "Hand overlay color", 0xFFFF8A9A));
    private final DoubleSetting intensity = addSetting(new DoubleSetting(
            "intensity", "Intensity", "How strongly the tint is applied", 0.35, 0.0, 1.0));

    public HandShaderModule() {
        super("hand-shader", "Hand Shader", "Tints your first-person hand and held item",
                ModuleCategory.QOL);
        listen(ClientTickEvent.class, event -> sync());
    }

    @Override
    protected void onEnable() {
        HandShaderState.setActive(true);
        sync();
    }

    @Override
    protected void onDisable() {
        HandShaderState.reset();
    }

    private void sync() {
        HandShaderState.setArgb(tint.get());
        HandShaderState.setIntensity((float) intensity.get());
    }
}
