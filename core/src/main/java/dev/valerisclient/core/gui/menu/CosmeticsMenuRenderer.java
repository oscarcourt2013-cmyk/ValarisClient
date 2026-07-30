package dev.valerisclient.core.gui.menu;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.cosmetics.CosmeticItem;
import dev.valerisclient.core.cosmetics.CosmeticManager;
import dev.valerisclient.core.cosmetics.CosmeticType;
import dev.valerisclient.core.design.PrimeDesign;
import dev.valerisclient.core.gui.GuiLayout;
import dev.valerisclient.core.gui.UiChrome;
import dev.valerisclient.core.i18n.PrimeLang;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.util.ColorUtil;

/** In-client cosmetics inventory — cape + wings only (world-rendered). */
public final class CosmeticsMenuRenderer {

    private static final CosmeticType[] SLOTS = {CosmeticType.CAPE, CosmeticType.WINGS};
    private static final int PANEL_W = 380;
    private static final int PANEL_H = 240;
    private static final int PREVIEW_W = 96;

    private CosmeticType slot = CosmeticType.CAPE;
    private int scrollIndex;

    public void render(RenderContext ctx, Theme theme, CosmeticManager cosmetics,
                       int screenW, int screenH, double mouseX, double mouseY) {
        int w = GuiLayout.fitToScreen(PANEL_W, screenW, 6, 240);
        int h = GuiLayout.fitToScreen(PANEL_H, screenH, 6, 170);
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        UiChrome.glassPanel(ctx, theme, x, y, w, h);
        GuiLayout.label(ctx, PrimeLang.get("prime.gui.cosmetics.title", "Cosmetics"), x + 12, y + 10, theme.accent());

        int tabX = x + 8;
        for (CosmeticType type : SLOTS) {
            int tw = GuiLayout.tabWidth(ctx, PrimeLang.enumValue(type), 10);
            boolean sel = type == slot;
            ctx.fillRoundedRect(tabX, y + 26, tw, 14, PrimeDesign.RADIUS_SM,
                    sel ? theme.surfaceElevated() : theme.backgroundLight());
            GuiLayout.label(ctx, PrimeLang.enumValue(type), tabX + 4, y + 29, sel ? theme.accent() : theme.foregroundMuted());
            tabX += tw + 4;
        }

        // Preview chrome
        int previewX = x + w - PREVIEW_W - 14;
        int previewY = y + 48;
        int previewH = h - 72;
        UiChrome.card(ctx, theme, previewX, previewY, PREVIEW_W, previewH, false);
        CosmeticItem equipped = cosmetics.equipped(slot);
        int tint = equipped != null ? equipped.tintArgb() : ColorUtil.withAlpha(theme.backgroundLight(), 0.9f);
        ctx.fillRoundedRect(previewX + 14, previewY + 18, PREVIEW_W - 28, previewH - 48,
                PrimeDesign.RADIUS_MD, tint);
        ctx.fillGradientVertical(previewX + 14, previewY + 18, PREVIEW_W - 28, previewH - 48,
                ColorUtil.withAlpha(0xFFFFFFFF, 0.18f), ColorUtil.withAlpha(0x00000000, 0f));
        String previewLabel = equipped != null
                ? GuiLayout.trimToWidth(ctx, equipped.name(), PREVIEW_W - 12)
                : PrimeLang.get("prime.gui.cosmetics.none", "None");
        GuiLayout.label(ctx, previewLabel, previewX + 8, previewY + previewH - 22, theme.foregroundMuted());

        int listW = w - PREVIEW_W - 28;
        int rowY = y + 48;
        ctx.pushClip(x + 4, rowY, listW, h - 80);
        int shown = 0;
        for (CosmeticItem item : cosmetics.catalog().values()) {
            if (item.type() != slot) {
                continue;
            }
            if (shown++ < scrollIndex) {
                continue;
            }
            if (rowY > y + h - 40) {
                break;
            }
            boolean isEquipped = equipped != null && equipped.id().equals(item.id());
            boolean hover = mouseX >= x + 8 && mouseX < x + 8 + listW - 8
                    && mouseY >= rowY && mouseY < rowY + 26;
            UiChrome.cardLite(ctx, theme, x + 8, rowY, listW - 8, 26, isEquipped || hover);
            ctx.fillRoundedRect(x + 12, rowY + 5, 16, 16, 4, item.tintArgb());
            ctx.fillRect(x + 12, rowY + 5, 16, 2, theme.accent());
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, item.name(), listW - 90),
                    x + 34, rowY + 5, theme.foreground());
            GuiLayout.label(ctx, PrimeLang.enumValue(item.rarity()), x + listW - 70, rowY + 5, theme.foregroundMuted());
            if (isEquipped) {
                GuiLayout.label(ctx, PrimeLang.get("prime.gui.cosmetics.equipped", "Equipped"),
                        x + 34, rowY + 14, theme.accent());
            }
            rowY += 28;
        }
        ctx.popClip();

        GuiLayout.label(ctx,
                PrimeLang.get("prime.gui.cosmetics.footer", "Visible to you + Valeris peers · Click to equip"),
                x + 12, y + h - 16, theme.foregroundMuted());
    }

    public boolean mousePressed(RenderContext ctx, double mx, double my, int screenW, int screenH, CosmeticManager cosmetics) {
        int w = GuiLayout.fitToScreen(PANEL_W, screenW, 6, 240);
        int h = GuiLayout.fitToScreen(PANEL_H, screenH, 6, 170);
        int x = (screenW - w) / 2;
        int y = (screenH - h) / 2;
        int tabX = x + 8;
        for (CosmeticType type : SLOTS) {
            int tw = GuiLayout.tabWidth(ctx, PrimeLang.enumValue(type), 10);
            if (mx >= tabX && mx < tabX + tw && my >= y + 26 && my < y + 40) {
                slot = type;
                scrollIndex = 0;
                return true;
            }
            tabX += tw + 4;
        }
        int listW = w - PREVIEW_W - 28;
        int rowY = y + 48;
        for (CosmeticItem item : cosmetics.catalog().values()) {
            if (item.type() != slot) {
                continue;
            }
            if (mx >= x + 8 && mx < x + 8 + listW - 8 && my >= rowY && my < rowY + 26) {
                CosmeticItem current = cosmetics.equipped(slot);
                if (current != null && current.id().equals(item.id())) {
                    cosmetics.unequip(slot);
                } else {
                    cosmetics.equip(slot, item.id());
                }
                return true;
            }
            rowY += 28;
            if (rowY > y + h - 40) {
                break;
            }
        }
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public boolean scroll(double delta) {
        scrollIndex = Math.max(0, scrollIndex - (int) delta);
        return true;
    }
}
