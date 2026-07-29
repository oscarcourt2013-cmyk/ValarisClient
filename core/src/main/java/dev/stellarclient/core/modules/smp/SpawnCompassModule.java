package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.modules.qol.WaypointHud;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Direction and distance to world spawn. */
public final class SpawnCompassModule extends Module {

    private final Element element;

    public SpawnCompassModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("spawn-compass", "Spawn Compass", "Direction and distance to world spawn", ModuleCategory.SURVIVAL);
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
        private String text = "Spawn: ?";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("spawn-compass", "Spawn Compass", HudAnchor.TOP_RIGHT, -4, 84);
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
            refresh();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh() {
            if (!adapter.hasPlayer()) {
                text = "Spawn: ?";
                return;
            }
            double dx = adapter.worldSpawnX() - adapter.playerX();
            double dz = adapter.worldSpawnZ() - adapter.playerZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            String dir = WaypointHud.directionTo(adapter, dx, dz);
            text = "Spawn: " + dist + "m " + dir;
        }
    }
}
