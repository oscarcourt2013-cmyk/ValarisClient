package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.event.PlayerDamageEvent;
import dev.valarisclient.core.hud.HudAnchor;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.theme.ThemeManager;

/** Brief floating damage-taken indicator HUD. */
public final class DamageIndicatorModule extends Module {

    private static final long DISPLAY_MILLIS = 1500;

    private final Element element;
    private float lastDamage;
    private long showUntilMillis;

    public DamageIndicatorModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("damage-indicator", "Damage Indicator", "Shows damage taken as floating text", ModuleCategory.PVP);
        listen(PlayerDamageEvent.class, event -> showDamage(event.amount()));
        this.element = hud.register(new Element(themes, adapter, this::snapshot));
        element.setVisible(false);
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        showUntilMillis = 0;
    }

    private void showDamage(float amount) {
        lastDamage = amount;
        showUntilMillis = System.currentTimeMillis() + DISPLAY_MILLIS;
    }

    private DamageSnapshot snapshot(long nowMillis) {
        if (nowMillis >= showUntilMillis) {
            return DamageSnapshot.hidden();
        }
        return new DamageSnapshot(true, lastDamage);
    }

    private static final class Element extends HudElement {
        private static final int PADDING = 3;

        private final ThemeManager themes;
        private final SnapshotSupplier snapshotSupplier;

        private boolean lastVisible;
        private float lastAmount = -1;
        private String text = "";

        Element(ThemeManager themes, MinecraftAdapter adapter, SnapshotSupplier snapshotSupplier) {
            super("damage", "Damage Indicator", HudAnchor.CENTER, 0, -30);
            this.themes = themes;
            this.snapshotSupplier = snapshotSupplier;
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
            refresh(nowMillis);
            if (text.isEmpty()) {
                return;
            }
            Theme theme = themes.active();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, PADDING, PADDING, theme.error(), true);
        }

        private void refresh() {
            refresh(System.currentTimeMillis());
        }

        private void refresh(long nowMillis) {
            DamageSnapshot snapshot = snapshotSupplier.get(nowMillis);
            if (!snapshot.visible()) {
                if (lastVisible) {
                    lastVisible = false;
                    text = "";
                }
                return;
            }
            float amount = snapshot.amount();
            if (!lastVisible || amount != lastAmount) {
                lastVisible = true;
                lastAmount = amount;
                text = "-" + formatDamage(amount);
            }
        }
    }

    private record DamageSnapshot(boolean visible, float amount) {
        static DamageSnapshot hidden() {
            return new DamageSnapshot(false, 0);
        }
    }

    @FunctionalInterface
    private interface SnapshotSupplier {
        DamageSnapshot get(long nowMillis);
    }

    private static String formatDamage(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format("%.1f", value);
    }
}
