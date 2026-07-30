package dev.valerisclient.core.modules.creator;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.state.CinematicCameraState;
import dev.valerisclient.core.state.CinematicGridState;

/** Rule-of-thirds overlay for cinematic shots. */
public final class CinematicGridModule extends Module {

    private final BooleanSetting withCinematic =
            addSetting(new BooleanSetting("with-cinematic", "With cinematic", "Also show when cinematic camera is on", true));

    private final GridElement grid;

    public CinematicGridModule(HudManager hud) {
        super("cinematic-grid", "Cinematic Grid", "Rule-of-thirds overlay", ModuleCategory.CREATOR);
        this.grid = hud.register(new GridElement());
        grid.setVisible(false);
        listen(ClientTickEvent.class, event -> updateVisibility());
    }

    @Override
    protected void onEnable() {
        updateVisibility();
    }

    @Override
    protected void onDisable() {
        CinematicGridState.setActive(false);
        grid.setVisible(false);
    }

    private void updateVisibility() {
        boolean show = isEnabled() || (withCinematic.get() && CinematicCameraState.active());
        CinematicGridState.setActive(show);
        grid.setVisible(show);
    }

    private static final class GridElement extends HudElement {
        private static final int LINE = 0x66FFFFFF;

        GridElement() {
            super("cinematic-grid", "Cinematic Grid", HudAnchor.TOP_LEFT, 0, 0);
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return ctx.screenWidth();
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.screenHeight();
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            int w = ctx.screenWidth();
            int h = ctx.screenHeight();
            int thirdW = w / 3;
            int thirdH = h / 3;
            ctx.fillRect(thirdW, 0, 1, h, LINE);
            ctx.fillRect(thirdW * 2, 0, 1, h, LINE);
            ctx.fillRect(0, thirdH, w, 1, LINE);
            ctx.fillRect(0, thirdH * 2, w, 1, LINE);
        }
    }
}
