package dev.valarisclient.core.modules.qol;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.event.PlayerDeathEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/** Saves death coordinates and shows distance to last death location. */
public final class DeathWaypointModule extends Module {

    private final Element element;

    public DeathWaypointModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("death-waypoint", "Death Waypoint", "Shows distance to your last death", ModuleCategory.SURVIVAL);
        this.element = hud.register(new Element(themes, adapter));
        element.setVisible(false);
        listen(PlayerDeathEvent.class, event -> element.setDeath(event.x(), event.y(), event.z()));
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

        private boolean hasDeath;
        private double deathX;
        private double deathY;
        private double deathZ;
        private String text = "Death: none";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("death-waypoint", "Death Waypoint", HudAnchor.TOP_RIGHT, -4, 36);
            this.themes = themes;
            this.adapter = adapter;
        }

        void setDeath(double x, double y, double z) {
            hasDeath = true;
            deathX = x;
            deathY = y;
            deathZ = z;
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
            if (!hasDeath) {
                text = "Death: none";
                return;
            }
            if (!adapter.hasPlayer()) {
                text = "Death: ?";
                return;
            }
            double dx = deathX - adapter.playerX();
            double dy = deathY - adapter.playerY();
            double dz = deathZ - adapter.playerZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            String dir = WaypointHud.directionTo(adapter, dx, dz);
            text = "Death: " + dist + "m " + dir;
        }
    }
}
