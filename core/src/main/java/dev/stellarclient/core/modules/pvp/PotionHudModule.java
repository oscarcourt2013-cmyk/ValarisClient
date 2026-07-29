package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Active potion effects HUD. */
public final class PotionHudModule extends Module {

    private final Element element;

    public PotionHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("potion-hud", "Potion HUD", "Lists your active potion effects", ModuleCategory.PVP);
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
        private static final int MAX_LINES = 8;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;
        private final String[] lines = new String[MAX_LINES];

        private int lineCount;
        private int cachedWidth;
        private int cachedHeight;

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("potions", "Potion HUD", HudAnchor.TOP_RIGHT, -4, 20);
            this.themes = themes;
            this.adapter = adapter;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            refresh(ctx);
            return cachedWidth;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            refresh(ctx);
            return cachedHeight;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            refresh(ctx);
            if (lineCount == 0) {
                return;
            }
            Theme theme = themes.active();
            ctx.fillRect(0, 0, cachedWidth, cachedHeight, theme.background());
            int lineHeight = ctx.fontHeight();
            for (int i = 0; i < lineCount; i++) {
                ctx.drawText(lines[i], PADDING, PADDING + i * lineHeight, theme.foreground(), true);
            }
        }

        private void refresh(RenderContext ctx) {
            if (!adapter.hasPlayer()) {
                if (lineCount != 0) {
                    lineCount = 0;
                    cachedWidth = 0;
                    cachedHeight = 0;
                }
                return;
            }
            int count = Math.min(adapter.potionEffectCount(), MAX_LINES);
            boolean changed = count != lineCount;
            int maxWidth = 0;
            for (int i = 0; i < count; i++) {
                String line = formatLine(i);
                if (!changed && !line.equals(lines[i])) {
                    changed = true;
                }
                lines[i] = line;
                maxWidth = Math.max(maxWidth, ctx.textWidth(line));
            }
            if (count < lineCount) {
                changed = true;
                for (int i = count; i < lineCount; i++) {
                    lines[i] = "";
                }
            }
            if (changed) {
                lineCount = count;
                cachedWidth = count == 0 ? 0 : maxWidth + PADDING * 2;
                cachedHeight = count == 0 ? 0 : count * ctx.fontHeight() + PADDING * 2;
            }
        }

        private String formatLine(int index) {
            String name = adapter.potionName(index);
            int seconds = adapter.potionDurationSeconds(index);
            int amplifier = adapter.potionAmplifier(index);
            if (amplifier > 0) {
                return name + " " + (amplifier + 1) + " (" + seconds + "s)";
            }
            return name + " (" + seconds + "s)";
        }
    }
}
