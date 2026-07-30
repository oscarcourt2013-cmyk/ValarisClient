package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.stream.StreamerPrivacyState;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/** Player block coordinates HUD. */
public final class CoordinatesModule extends Module {

    private final Element element;

    public CoordinatesModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("coordinates", "Coordinates HUD", "Shows your block position", ModuleCategory.PVP);
        this.element = hud.register(new Element(themes, adapter));
        element.setVisible(false);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;

        // Rebuilt only when the block position changes.
        private int lastX = Integer.MIN_VALUE;
        private int lastY = Integer.MIN_VALUE;
        private int lastZ = Integer.MIN_VALUE;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("coordinates", "Coordinates", HudAnchor.BOTTOM_LEFT, 4, -4);
            this.themes = themes;
            this.adapter = adapter;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            refresh();
            return ctx.textWidth(text) + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh() {
            if (StreamerPrivacyState.hudShield() || StreamerPrivacyState.blockLocationHud()) {
                text = "§8[hidden]";
                return;
            }
            if (!adapter.hasPlayer()) {
                if (text.isEmpty()) {
                    text = "X: ? Y: ? Z: ?";
                }
                return;
            }
            int x = (int) Math.floor(adapter.playerX());
            int y = (int) Math.floor(adapter.playerY());
            int z = (int) Math.floor(adapter.playerZ());
            if (x != lastX || y != lastY || z != lastZ) {
                lastX = x;
                lastY = y;
                lastZ = z;
                text = "X: " + x + " Y: " + y + " Z: " + z;
            }
        }
    }
}
