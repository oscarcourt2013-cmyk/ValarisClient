package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/**
 * Visual WASD + mouse button overlay. Key state is polled per rendered frame
 * from GLFW through the adapter — a handful of native calls, no listeners.
 */
public final class KeystrokesModule extends Module {

    private final BooleanSetting showMouse =
            addSetting(new BooleanSetting("show-mouse", "Mouse buttons", "Show LMB/RMB row", true));

    private final Element element;

    public KeystrokesModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("keystrokes", "Keystrokes", "Shows your WASD and mouse inputs", ModuleCategory.PVP);
        this.element = hud.register(new Element(themes, adapter, showMouse));
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
        private static final int KEY_SIZE = 18;
        private static final int GAP = 2;
        private static final int MOUSE_HEIGHT = 12;

        // GLFW codes: W A S D.
        private static final int KEY_W = 87;
        private static final int KEY_A = 65;
        private static final int KEY_S = 83;
        private static final int KEY_D = 68;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;
        private final BooleanSetting showMouse;

        Element(ThemeManager themes, MinecraftAdapter adapter, BooleanSetting showMouse) {
            super("keystrokes", "Keystrokes", HudAnchor.MIDDLE_LEFT, 8, 40);
            this.themes = themes;
            this.adapter = adapter;
            this.showMouse = showMouse;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return KEY_SIZE * 3 + GAP * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            int height = KEY_SIZE * 2 + GAP;
            if (showMouse.get()) {
                height += GAP + MOUSE_HEIGHT;
            }
            return height;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            int col2 = KEY_SIZE + GAP;
            int col3 = (KEY_SIZE + GAP) * 2;
            int row2 = KEY_SIZE + GAP;

            key(ctx, theme, "W", col2, 0, KEY_SIZE, KEY_SIZE, adapter.isKeyDown(KEY_W));
            key(ctx, theme, "A", 0, row2, KEY_SIZE, KEY_SIZE, adapter.isKeyDown(KEY_A));
            key(ctx, theme, "S", col2, row2, KEY_SIZE, KEY_SIZE, adapter.isKeyDown(KEY_S));
            key(ctx, theme, "D", col3, row2, KEY_SIZE, KEY_SIZE, adapter.isKeyDown(KEY_D));

            if (showMouse.get()) {
                int mouseY = row2 + KEY_SIZE + GAP;
                int mouseWidth = (KEY_SIZE * 3 + GAP * 2 - GAP) / 2;
                key(ctx, theme, "LMB", 0, mouseY, mouseWidth, MOUSE_HEIGHT, adapter.isMouseButtonDown(0));
                key(ctx, theme, "RMB", mouseWidth + GAP, mouseY, mouseWidth, MOUSE_HEIGHT, adapter.isMouseButtonDown(1));
            }
        }

        private void key(RenderContext ctx, Theme theme, String label,
                         int x, int y, int width, int height, boolean pressed) {
            ctx.fillRect(x, y, width, height, pressed ? theme.accent() : theme.background());
            int labelColor = pressed ? theme.background() : theme.foreground();
            ctx.drawText(label, x + (width - ctx.textWidth(label)) / 2, y + (height - ctx.fontHeight()) / 2 + 1,
                    labelColor, false);
        }
    }
}
