package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;
import dev.stellarclient.core.util.ColorUtil;

/**
 * Horizontal 4-slot armor strip: Helmet → Chestplate → Leggings → Boots,
 * with durability bars and critical !!! badges in Prime chrome.
 */
public final class ArmorHudModule extends Module {

    private final BooleanSetting showDurability = addSetting(new BooleanSetting(
            "durability", "Durability bars", "Show durability under damaged pieces", true));
    private final BooleanSetting showGhosts = addSetting(new BooleanSetting(
            "ghosts", "Empty ghosts", "Show faint outlines for empty armor slots", true));
    private final BooleanSetting showWarnings = addSetting(new BooleanSetting(
            "warnings", "Critical badges", "Show !!! when a piece is critically damaged", true));
    private final IntSetting warningThreshold = addSetting(new IntSetting(
            "warn-threshold", "Warning %", "Durability % that triggers the critical badge", 20, 5, 50));

    private final Element element;

    public ArmorHudModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("armor-hud", "Armor HUD", "Shows equipped armor durability", ModuleCategory.PVP);
        this.element = hud.register(new Element(
                themes, adapter, showDurability, showGhosts, showWarnings, warningThreshold));
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
        /** Adapter order is boots→helmet; display order is helmet→boots. */
        private static final int[] DISPLAY_SLOTS = {3, 2, 1, 0};

        private static final int SLOT = 20;
        private static final int ITEM = 16;
        private static final int PAD = 2;
        private static final int BAR_H = 2;
        private static final int BAR_GAP = 1;
        private static final int BADGE_SPACE = 12;
        private static final int STRIP_H = PAD + ITEM + BAR_GAP + BAR_H + PAD;

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;
        private final BooleanSetting showDurability;
        private final BooleanSetting showGhosts;
        private final BooleanSetting showWarnings;
        private final IntSetting warningThreshold;

        Element(ThemeManager themes, MinecraftAdapter adapter,
                BooleanSetting showDurability, BooleanSetting showGhosts,
                BooleanSetting showWarnings, IntSetting warningThreshold) {
            super("armor", "Armor HUD", HudAnchor.BOTTOM_CENTER, 0, -72);
            this.themes = themes;
            this.adapter = adapter;
            this.showDurability = showDurability;
            this.showGhosts = showGhosts;
            this.showWarnings = showWarnings;
            this.warningThreshold = warningThreshold;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return SLOT * DISPLAY_SLOTS.length;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return BADGE_SPACE + STRIP_H;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            int width = measureWidth(ctx);
            int stripY = BADGE_SPACE;
            boolean hasPlayer = adapter.hasPlayer();

            drawStripFrame(ctx, theme, 0, stripY, width, STRIP_H);

            for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
                int slot = DISPLAY_SLOTS[i];
                int slotX = i * SLOT;
                int itemX = slotX + (SLOT - ITEM) / 2;
                int itemY = stripY + PAD;

                if (i > 0) {
                    ctx.fillRect(slotX, stripY + 2, 1, STRIP_H - 4,
                            ColorUtil.withAlpha(theme.border(), 0.45f));
                }

                boolean equipped = hasPlayer && adapter.hasArmor(slot);
                if (equipped) {
                    ctx.drawArmorItem(slot, itemX, itemY);
                    if (showDurability.get()) {
                        drawDurabilityBar(ctx, theme, slot, slotX, stripY);
                    }
                    if (showWarnings.get() && isCritical(slot)) {
                        drawWarningBadge(ctx, theme, slotX + SLOT / 2, stripY);
                    }
                } else if (showGhosts.get()) {
                    ctx.drawArmorGhost(slot, itemX, itemY);
                    // Mute the ghost so it reads as an outline, not a real item.
                    ctx.fillRect(itemX, itemY, ITEM, ITEM, ColorUtil.withAlpha(theme.background(), 0.55f));
                }
            }
        }

        private void drawStripFrame(RenderContext ctx, Theme theme, int x, int y, int w, int h) {
            int radius = PrimeDesign.RADIUS_SM;
            ctx.fillSoftShadow(x, y + 1, w, h, radius, 0x60000000);
            ctx.fillRoundedBorder(x, y, w, h, radius, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.85f),
                    ColorUtil.withAlpha(theme.background(), 0.92f));
            // Soft inner glow along the top edge (Prime accent, not metallic bevel).
            ctx.fillGradientHorizontal(x + radius, y + 1, w - radius * 2, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.55f),
                    ColorUtil.withAlpha(theme.accent(), 0.08f));
            ctx.fillRect(x + 1, y + h - 1, w - 2, 1,
                    ColorUtil.withAlpha(theme.accentSecondary(), 0.35f));
        }

        private void drawDurabilityBar(RenderContext ctx, Theme theme, int slot, int slotX, int stripY) {
            int max = adapter.armorMaxDurability(slot);
            if (max <= 0) {
                return;
            }
            int remaining = adapter.armorDurability(slot);
            if (remaining >= max) {
                return; // pristine — no bar
            }
            float ratio = Math.clamp(remaining / (float) max, 0f, 1f);
            int barW = SLOT - PAD * 2;
            int barX = slotX + PAD;
            int barY = stripY + PAD + ITEM + BAR_GAP;
            ctx.fillRect(barX, barY, barW, BAR_H, 0xE0000000);
            int fill = Math.max(1, Math.round(barW * ratio));
            ctx.fillRect(barX, barY, fill, BAR_H, durabilityColor(theme, ratio));
        }

        private int durabilityColor(Theme theme, float ratio) {
            float warn = warningThreshold.get() / 100f;
            if (ratio <= warn) {
                return theme.error();
            }
            // Green when healthy → amber mid → red near critical.
            if (ratio >= 0.55f) {
                return theme.success();
            }
            float t = (ratio - warn) / Math.max(0.01f, 0.55f - warn);
            return ColorUtil.lerp(theme.error(), theme.success(), t);
        }

        private boolean isCritical(int slot) {
            int max = adapter.armorMaxDurability(slot);
            if (max <= 0) {
                return false;
            }
            int remaining = adapter.armorDurability(slot);
            return remaining * 100 / max <= warningThreshold.get();
        }

        private void drawWarningBadge(RenderContext ctx, Theme theme, int centerX, int stripY) {
            int size = 10;
            int x = centerX - size / 2;
            int y = stripY - size - 1;
            ctx.fillRoundedRect(x - 1, y - 1, size + 2, size + 2, size / 2 + 1,
                    ColorUtil.withAlpha(theme.accent(), 0.35f));
            ctx.fillRoundedRect(x, y, size, size, size / 2, theme.error());
            String mark = "!!!";
            float scale = 0.55f;
            int tw = ctx.smoothTextWidth(mark, scale);
            int tx = centerX - tw / 2;
            int ty = y + (size - Math.round(ctx.fontHeight() * scale)) / 2;
            ctx.drawSmoothText(mark, tx, ty, theme.foreground(), scale);
        }
    }
}
