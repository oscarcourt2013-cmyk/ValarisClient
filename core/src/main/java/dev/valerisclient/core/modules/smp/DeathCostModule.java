package dev.valerisclient.core.modules.smp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.event.PlayerDeathEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.theme.ThemeManager;

/** Tracks XP lost on death and shows a recovery reminder. */
public final class DeathCostModule extends Module {

    private final Element element;
    private final MinecraftAdapter adapter;
    private int lastXpLevel;

    public DeathCostModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("death-cost", "Death Cost", "Shows XP lost and death location reminder", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes, adapter));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> trackXp());
        listen(PlayerDeathEvent.class, event -> element.recordDeath(lastXpLevel, event.x(), event.y(), event.z()));
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        lastXpLevel = adapter.playerXpLevel();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void trackXp() {
        lastXpLevel = adapter.playerXpLevel();
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;

        private boolean hasDeath;
        private int xpLost;
        private double deathX;
        private double deathY;
        private double deathZ;
        private String text = "Death cost: none";

        Element(ThemeManager themes, MinecraftAdapter adapter) {
            super("death-cost", "Death Cost", HudAnchor.TOP_RIGHT, -4, 68);
            this.themes = themes;
            this.adapter = adapter;
        }

        void recordDeath(int xpBeforeDeath, double x, double y, double z) {
            hasDeath = true;
            xpLost = Math.max(0, xpBeforeDeath);
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
                text = "Death cost: none";
                return;
            }
            int x = (int) Math.floor(deathX);
            int y = (int) Math.floor(deathY);
            int z = (int) Math.floor(deathZ);
            text = "Lost " + xpLost + " XP @ " + x + " " + y + " " + z;
        }
    }
}
