package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Lists saved servers and connects to the selected index on enable. */
public final class ServerSwitcherModule extends Module {

    private final IntSetting serverIndex =
            addSetting(new IntSetting("server-index", "Server index", "Server to connect to", 0, 0, 50));

    private final Element element;
    private final MinecraftAdapter adapter;

    public ServerSwitcherModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("server-switcher", "Server Switcher", "Quick-connect to a saved server", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes, adapter, serverIndex));
        element.setVisible(false);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        adapter.connectToServer(serverIndex.get());
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;
        private static final int LINE_GAP = 1;
        private static final int MAX_LINES = 6;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;
        private final IntSetting serverIndex;

        private final String[] lines = new String[MAX_LINES];
        private int lineCount;
        private int lastCount = -1;
        private int lastSelected = -1;

        Element(ThemeManager themes, MinecraftAdapter adapter, IntSetting serverIndex) {
            super("server-switcher", "Server Switcher", HudAnchor.MIDDLE_LEFT, 8, -40);
            this.themes = themes;
            this.adapter = adapter;
            this.serverIndex = serverIndex;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            refresh();
            int max = 0;
            for (int i = 0; i < lineCount; i++) {
                max = Math.max(max, ctx.textWidth(lines[i]));
            }
            return max + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            refresh();
            if (lineCount == 0) {
                return ctx.fontHeight() + PADDING * 2;
            }
            return lineCount * ctx.fontHeight() + (lineCount - 1) * LINE_GAP + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            refresh();
            int width = measureWidth(ctx);
            int height = measureHeight(ctx);
            ctx.fillRect(0, 0, width, height, theme.background());
            int y = PADDING;
            int selected = serverIndex.get();
            for (int i = 0; i < lineCount; i++) {
                int color = i == selected ? theme.accent() : theme.foreground();
                ctx.drawText(lines[i], PADDING, y, color, true);
                y += ctx.fontHeight() + LINE_GAP;
            }
        }

        private void refresh() {
            int count = adapter.serverListCount();
            int selected = serverIndex.get();
            if (count == lastCount && selected == lastSelected) {
                return;
            }
            lastCount = count;
            lastSelected = selected;
            lineCount = 0;
            if (count == 0) {
                lines[0] = "No servers";
                lineCount = 1;
                return;
            }
            int limit = Math.min(count, MAX_LINES);
            for (int i = 0; i < limit; i++) {
                String name = adapter.serverListName(i);
                if (name.isEmpty()) {
                    name = adapter.serverListAddress(i);
                }
                lines[i] = (i == selected ? "> " : "  ") + name;
            }
            lineCount = limit;
        }
    }
}
