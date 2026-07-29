package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.event.AttackEntityEvent;
import dev.stellarclient.core.event.PlayerDamageEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/** Consecutive-hit counter HUD. */
public final class ComboCounterModule extends Module {

    private final Element element;
    private int combo;

    public ComboCounterModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("combo-counter", "Combo Counter", "Tracks consecutive hits without taking damage", ModuleCategory.PVP);
        listen(AttackEntityEvent.class, event -> combo++);
        listen(PlayerDamageEvent.class, event -> combo = 0);
        this.element = hud.register(new Element(themes, adapter, this::combo));
        element.setVisible(false);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        combo = 0;
    }

    private int combo() {
        return combo;
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private final ComboSupplier comboSupplier;

        private int lastCombo = -1;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter, ComboSupplier comboSupplier) {
            super("combo", "Combo Counter", HudAnchor.TOP_RIGHT, -4, 4);
            this.themes = themes;
            this.comboSupplier = comboSupplier;
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
            refresh();
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.foreground(), true);
        }

        private void refresh() {
            int combo = comboSupplier.get();
            if (combo != lastCombo) {
                lastCombo = combo;
                text = combo + " Combo";
            }
        }
    }

    @FunctionalInterface
    private interface ComboSupplier {
        int get();
    }
}
