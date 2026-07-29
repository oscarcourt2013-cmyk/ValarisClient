package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Single waypoint with distance and direction HUD. Press B to save current position. */
public final class WaypointsModule extends Module {

    private static final int KEY_SAVE = 66;

    private final StringSetting name =
            addSetting(new StringSetting("name", "Name", "Waypoint label", "Waypoint"));
    private final DoubleSetting x =
            addSetting(new DoubleSetting("x", "X", "Waypoint X coordinate", 0, -30_000_000, 30_000_000));
    private final DoubleSetting y =
            addSetting(new DoubleSetting("y", "Y", "Waypoint Y coordinate", 64, -64, 320));
    private final DoubleSetting z =
            addSetting(new DoubleSetting("z", "Z", "Waypoint Z coordinate", 0, -30_000_000, 30_000_000));

    private final Element element;
    private final MinecraftAdapter adapter;
    private boolean saveKeyDown;

    public WaypointsModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("waypoints", "Waypoints", "Shows distance and direction to a saved waypoint", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes, adapter, name, x, y, z));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> onTick());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        saveKeyDown = false;
    }

    private void onTick() {
        boolean down = adapter.isKeyDown(KEY_SAVE);
        if (down && !saveKeyDown && adapter.hasPlayer()) {
            x.set(adapter.playerX());
            y.set(adapter.playerY());
            z.set(adapter.playerZ());
        }
        saveKeyDown = down;
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;
        private final StringSetting name;
        private final DoubleSetting x;
        private final DoubleSetting y;
        private final DoubleSetting z;

        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter,
                StringSetting name, DoubleSetting x, DoubleSetting y, DoubleSetting z) {
            super("waypoints", "Waypoints", HudAnchor.TOP_RIGHT, -4, 20);
            this.themes = themes;
            this.adapter = adapter;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
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
                text = name.get() + ": ?";
                return;
            }
            double dx = x.get() - adapter.playerX();
            double dy = y.get() - adapter.playerY();
            double dz = z.get() - adapter.playerZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            String dir = WaypointHud.directionTo(adapter, dx, dz);
            text = name.get() + ": " + dist + "m " + dir;
        }
    }
}
