package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Lists shulker box contents when hovering a shulker item. */
public final class ShulkerPreviewModule extends Module {

    private final Element element;

    public ShulkerPreviewModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("shulker-preview", "Shulker Preview", "Shows shulker contents on hover", ModuleCategory.QOL);
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
        private static final int LINE_GAP = 1;
        private static final int MAX_LINES = 8;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;

        private final String[] lines = new String[MAX_LINES];
        private int lineCount;

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("shulker-preview", "Shulker Preview", HudAnchor.MIDDLE_RIGHT, -8, 0);
            this.themes = themes;
            this.adapter = adapter;
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
            for (int i = 0; i < lineCount; i++) {
                ctx.drawText(lines[i], PADDING, y, theme.foreground(), true);
                y += ctx.fontHeight() + LINE_GAP;
            }
        }

        private void refresh() {
            lineCount = 0;
            if (!adapter.hoveredItemIsShulkerBox()) {
                lines[0] = "No shulker";
                lineCount = 1;
                return;
            }
            int slots = adapter.shulkerSlotCount();
            for (int i = 0; i < slots && lineCount < MAX_LINES; i++) {
                String item = adapter.shulkerSlotItem(i);
                if (item.isEmpty()) {
                    continue;
                }
                int count = adapter.shulkerSlotCount(i);
                lines[lineCount++] = count > 1 ? item + " x" + count : item;
            }
            if (lineCount == 0) {
                lines[0] = "Empty shulker";
                lineCount = 1;
            }
        }
    }
}
