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

/** Active combat target name and health HUD. */
public final class TargetHudModule extends Module {

    private final Element element;

    public TargetHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("target-hud", "Target HUD", "Shows your current target's name and health", ModuleCategory.PVP);
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

        private boolean lastHasTarget;
        private String lastName = "";
        private float lastHealth = -1;
        private float lastMaxHealth = -1;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("target", "Target HUD", HudAnchor.TOP_CENTER, 0, 4);
            this.themes = themes;
            this.adapter = adapter;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            refresh();
            if (text.isEmpty()) {
                return 0;
            }
            return ctx.textWidth(text) + PADDING * 2;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            refresh();
            if (text.isEmpty()) {
                return 0;
            }
            return ctx.fontHeight() + PADDING * 2;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            refresh();
            if (text.isEmpty()) {
                return;
            }
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh() {
            boolean hasTarget = adapter.hasTarget();
            if (!hasTarget) {
                if (lastHasTarget) {
                    lastHasTarget = false;
                    text = "";
                }
                return;
            }
            String name = adapter.targetName();
            float health = adapter.targetHealth();
            float maxHealth = adapter.targetMaxHealth();
            if (!lastHasTarget || !name.equals(lastName) || health != lastHealth || maxHealth != lastMaxHealth) {
                lastHasTarget = true;
                lastName = name;
                lastHealth = health;
                lastMaxHealth = maxHealth;
                text = name + " " + formatHealth(health) + "/" + formatHealth(maxHealth);
            }
        }

        private static String formatHealth(float value) {
            return value == (int) value ? Integer.toString((int) value) : String.format("%.1f", value);
        }
    }
}
