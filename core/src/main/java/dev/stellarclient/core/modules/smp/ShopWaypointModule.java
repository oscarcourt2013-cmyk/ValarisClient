package dev.stellarclient.core.modules.smp;

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
import dev.stellarclient.core.modules.qol.WaypointHud;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Shop waypoint with distance and direction. Press B to save current position. */
public final class ShopWaypointModule extends Module {

    private static final int KEY_SAVE = 66;

    private final StringSetting shopName =
            addSetting(new StringSetting("shop-name", "Shop name", "Label for this shop waypoint", "Shop"));
    private final DoubleSetting x =
            addSetting(new DoubleSetting("x", "X", "Shop X coordinate", 0, -30_000_000, 30_000_000));
    private final DoubleSetting y =
            addSetting(new DoubleSetting("y", "Y", "Shop Y coordinate", 64, -64, 320));
    private final DoubleSetting z =
            addSetting(new DoubleSetting("z", "Z", "Shop Z coordinate", 0, -30_000_000, 30_000_000));

    private final Element element;
    private final MinecraftAdapter adapter;
    private boolean saveKeyDown;

    public ShopWaypointModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("shop-waypoint", "Shop Waypoint", "Navigate to a saved shop location", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes, adapter, shopName, x, y, z));
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
        private final StringSetting shopName;
        private final DoubleSetting x;
        private final DoubleSetting y;
        private final DoubleSetting z;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter,
                StringSetting shopName, DoubleSetting x, DoubleSetting y, DoubleSetting z) {
            super("shop-waypoint", "Shop Waypoint", HudAnchor.TOP_RIGHT, -4, 52);
            this.themes = themes;
            this.adapter = adapter;
            this.shopName = shopName;
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
                text = shopName.get() + ": ?";
                return;
            }
            double dx = x.get() - adapter.playerX();
            double dy = y.get() - adapter.playerY();
            double dz = z.get() - adapter.playerZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
            String dir = WaypointHud.directionTo(adapter, dx, dz);
            text = shopName.get() + ": " + dist + "m " + dir;
        }
    }
}
