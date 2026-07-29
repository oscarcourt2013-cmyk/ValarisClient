package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Coordinates plus biome name for resource finding. */
public final class BiomeCoordsModule extends Module {

    private final Element element;

    public BiomeCoordsModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("biome-coords", "Biome Coords", "Shows position and current biome", ModuleCategory.SURVIVAL);
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

        private int lastX = Integer.MIN_VALUE;
        private int lastY = Integer.MIN_VALUE;
        private int lastZ = Integer.MIN_VALUE;
        private String lastBiome = "";
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("biome-coords", "Biome Coords", HudAnchor.BOTTOM_LEFT, 4, -4);
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
                text = "X: ? Y: ? Z: ? | ?";
                return;
            }
            int x = (int) Math.floor(adapter.playerX());
            int y = (int) Math.floor(adapter.playerY());
            int z = (int) Math.floor(adapter.playerZ());
            String biome = adapter.biomeName();
            if (biome.isEmpty()) {
                biome = "?";
            }
            if (x != lastX || y != lastY || z != lastZ || !biome.equals(lastBiome)) {
                lastX = x;
                lastY = y;
                lastZ = z;
                lastBiome = biome;
                text = "X:" + x + " Y:" + y + " Z:" + z + " | " + biome;
            }
        }
    }
}
