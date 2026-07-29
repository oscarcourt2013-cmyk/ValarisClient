package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.state.ZoomState;

/** Hold C to zoom while enabled. */
public final class ZoomModule extends Module {

    private static final int KEY_C = 67;

    private final DoubleSetting multiplier =
            addSetting(new DoubleSetting("multiplier", "Zoom level", "FOV multiplier while zooming", 0.25, 0.05, 1.0));

    private final MinecraftAdapter adapter;

    public ZoomModule(MinecraftAdapter adapter) {
        super("zoom", "Zoom", "Hold C to zoom the camera", ModuleCategory.QOL);
        this.adapter = adapter;
        listen(ClientTickEvent.class, event -> onTick());
    }

    @Override
    protected void onDisable() {
        ZoomState.reset();
    }

    private void onTick() {
        if (adapter.isKeyDown(KEY_C)) {
            ZoomState.setMultiplier((float) multiplier.get());
        } else {
            ZoomState.reset();
        }
    }
}
