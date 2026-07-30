package dev.valerisclient.core.modules.creator;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.state.ZoomState;

/** Hold-to-zoom camera separate from gameplay zoom (V key). */
public final class CameraZoomModule extends Module {

    private static final int KEY_V = 86;
    private static final float ZOOM_MULTIPLIER = 0.35f;

    private final MinecraftAdapter adapter;

    public CameraZoomModule(MinecraftAdapter adapter) {
        super("camera-zoom", "Camera Zoom", "Hold V to zoom the camera for shots", ModuleCategory.CREATOR);
        this.adapter = adapter;

        listen(ClientTickEvent.class, event -> updateZoom());
    }

    @Override
    protected void onDisable() {
        ZoomState.reset();
    }

    private void updateZoom() {
        if (adapter.isKeyDown(KEY_V)) {
            ZoomState.setMultiplier(ZOOM_MULTIPLIER);
        } else {
            ZoomState.reset();
        }
    }
}
