package dev.valarisclient.core.modules.creator;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.state.CinematicCameraState;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/** Smooths camera rotation by interpolating yaw and pitch each tick. */
public final class CinematicCameraModule extends Module {

    private static final float SMOOTH_FACTOR = 0.15f;

    private final MinecraftAdapter adapter;
    private final Element element;

    private float smoothedYaw;
    private float smoothedPitch;
    private boolean initialized;

    public CinematicCameraModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("cinematic-camera", "Cinematic Camera", "Smooths camera movement for recordings", ModuleCategory.CREATOR);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes));
        element.setVisible(false);

        listen(ClientTickEvent.class, event -> tickSmoothing());
    }

    @Override
    protected void onEnable() {
        initialized = false;
        CinematicCameraState.setActive(true);
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        CinematicCameraState.reset();
        element.setVisible(false);
    }

    private void tickSmoothing() {
        if (!adapter.hasPlayer()) {
            return;
        }
        float yaw = adapter.playerYaw();
        float pitch = adapter.playerPitch();
        if (!initialized) {
            smoothedYaw = yaw;
            smoothedPitch = pitch;
            initialized = true;
        } else {
            smoothedYaw = lerpAngle(smoothedYaw, yaw, SMOOTH_FACTOR);
            smoothedPitch = smoothedPitch + (pitch - smoothedPitch) * SMOOTH_FACTOR;
        }
        CinematicCameraState.setRotation(smoothedYaw, smoothedPitch);
    }

    private static float lerpAngle(float from, float to, float factor) {
        float delta = ((to - from + 540.0f) % 360.0f) - 180.0f;
        return from + delta * factor;
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;
        private static final String TEXT = "Cinematic ON";

        private final ThemeManager themes;

        Element(ThemeManager themes) {
            super("cinematic-indicator", "Cinematic Indicator", HudAnchor.TOP_RIGHT, -4, 4);
            this.themes = themes;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return ctx.textWidth(TEXT) + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(TEXT, PADDING, PADDING, theme.accent(), true);
        }
    }
}
