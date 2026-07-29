package dev.stellarclient.core.gui.menu;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.design.PrimeLogo;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;
import dev.stellarclient.core.util.Easing;

/**
 * Prime pause menu â€” dark desaturated world, red/black chrome, centered GAME MENU panel.
 */
public final class GameMenuRenderer {

    private static final GameMenuAction[] GRID = {
            GameMenuAction.ADVANCEMENTS, GameMenuAction.STATISTICS,
            GameMenuAction.GIVE_FEEDBACK, GameMenuAction.REPORT_BUGS,
            GameMenuAction.OPTIONS, GameMenuAction.OPEN_TO_LAN
    };

    private static final String[] GRID_KEYS = {
            "gui.advancements", "gui.stats",
            "menu.sendFeedback", "menu.reportBugs",
            "menu.options", "menu.shareToLan"
    };

    private static final String[] GRID_FALLBACKS = {
            "Advancements", "Statistics",
            "Give Feedback", "Report Bugs",
            "Options...", "Open to LAN"
    };

    private static final String[] GRID_ICONS = {
            "â˜…", "â–®", "âœ‰", "âš‘", "âš™", "â¬¡"
    };

    public void render(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                       double mouseX, double mouseY, float fade, float emberPhase) {
        float eased = Easing.easeOutCubic(fade);
        GameMenuLayout layout = GameMenuLayout.compute(ctx.screenWidth(), ctx.screenHeight());

        renderWorldWash(ctx, eased);
        renderEmbers(ctx, theme, eased, emberPhase);
        renderBranding(ctx, theme, layout, eased);
        renderPanel(ctx, theme, layout, eased);
        renderPrimary(ctx, theme, adapter, layout, mouseX, mouseY, eased);
        renderGrid(ctx, theme, adapter, layout, mouseX, mouseY, eased);
        renderQuit(ctx, theme, adapter, layout, mouseX, mouseY, eased);
        renderFooter(ctx, theme, layout, eased);
    }

    public GameMenuAction hitAction(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        GameMenuLayout layout = GameMenuLayout.compute(screenWidth, screenHeight);
        if (hit(mouseX, mouseY, layout.primaryX(), layout.primaryY(), layout.primaryW(), layout.primaryH())) {
            return GameMenuAction.BACK_TO_GAME;
        }
        if (hit(mouseX, mouseY, layout.quitX(), layout.quitY(), layout.quitW(), layout.quitH())) {
            return GameMenuAction.SAVE_AND_QUIT;
        }
        for (int i = 0; i < GRID.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = layout.gridCellX(col);
            int y = layout.gridCellY(row);
            if (hit(mouseX, mouseY, x, y, layout.cellW(), layout.cellH())) {
                return GRID[i];
            }
        }
        return null;
    }

    private void renderWorldWash(RenderContext ctx, float fade) {
        int w = ctx.screenWidth();
        int h = ctx.screenHeight();
        int a = Math.round(0xB0 * fade);
        ctx.fillRect(0, 0, w, h, (a << 24) | 0x0A0A0A);
        int edge = Math.max(40, h / 6);
        int topA = Math.round(0x70 * fade) << 24;
        ctx.fillGradientVertical(0, 0, w, edge, topA, 0);
        ctx.fillGradientVertical(0, h - edge - GameMenuLayout.FOOTER_H, w, edge, 0, topA);
    }

    private void renderEmbers(RenderContext ctx, Theme theme, float fade, float phase) {
        int w = ctx.screenWidth();
        int h = ctx.screenHeight();
        int accent = theme.accent();
        for (int i = 0; i < 28; i++) {
            float seed = i * 17.13f;
            float x = ((seed * 37.1f) % 1f) * w;
            float drift = (phase * (0.15f + (i % 5) * 0.04f) + seed) % 1f;
            float y = h - drift * (h * 0.85f);
            int size = 1 + (i % 3);
            float pulse = 0.35f + 0.45f * (0.5f + 0.5f * (float) Math.sin(phase * 2.2f + seed));
            int color = ColorUtil.withAlpha(accent, fade * pulse * 0.55f);
            ctx.fillRect(Math.round(x), Math.round(y), size, size, color);
        }
    }

    private void renderBranding(RenderContext ctx, Theme theme, GameMenuLayout layout, float fade) {
        ctx.setDrawOpacity(fade);
        int centerX = ctx.screenWidth() / 2;
        PrimeLogo.drawCentered(ctx, centerX, layout.logoY(), layout.logoH(), 0xFFFFFFFF);

        float primeScale = 1.05f;
        String prime = "PRIME";
        int primeW = ctx.smoothTextWidth(prime, primeScale);
        ctx.drawSmoothText(prime, centerX - primeW / 2, layout.brandY(), theme.foreground(), primeScale);

        float clientScale = 0.72f;
        String client = "C L I E N T";
        int clientW = ctx.smoothTextWidth(client, clientScale);
        ctx.drawSmoothText(client, centerX - clientW / 2, layout.brandY() + 12,
                theme.accent(), clientScale);

        int lineW = 72;
        int lineY = layout.dividerY();
        ctx.fillRect(centerX - lineW, lineY, lineW - 5, 1, ColorUtil.withAlpha(theme.accent(), 0.85f));
        ctx.fillRect(centerX + 5, lineY, lineW - 5, 1, ColorUtil.withAlpha(theme.accent(), 0.85f));
        // Red diamond
        ctx.fillRect(centerX - 2, lineY - 2, 4, 4, theme.accent());
        ctx.fillRect(centerX - 1, lineY - 3, 2, 6, theme.accent());

        String title = "GAME MENU";
        float titleScale = 0.78f;
        int titleW = ctx.smoothTextWidth(title, titleScale);
        ctx.drawSmoothText(title, centerX - titleW / 2, layout.titleY(),
                ColorUtil.withAlpha(theme.accent(), 0.95f), titleScale);
        ctx.setDrawOpacity(1f);
    }

    private void renderPanel(RenderContext ctx, Theme theme, GameMenuLayout layout, float fade) {
        ctx.setDrawOpacity(fade);
        int x = layout.panelX();
        int y = layout.panelY();
        int w = layout.panelW();
        int h = layout.panelH();
        int radius = PrimeDesign.RADIUS_MD;

        ctx.fillSoftShadow(x, y, w, h, radius, 0x90000000);
        ctx.fillRoundedBorder(x, y, w, h, radius, 1,
                ColorUtil.withAlpha(theme.accent(), 0.55f),
                ColorUtil.withAlpha(0xFF0C0C0E, 0.92f));

        // Glowing top / bottom energy lines
        int mid = (w - 20) / 2;
        ctx.fillGradientHorizontal(x + 10, y + 1, mid, 2,
                ColorUtil.withAlpha(theme.accent(), 0.12f),
                ColorUtil.withAlpha(theme.accent(), 0.95f));
        ctx.fillGradientHorizontal(x + 10 + mid, y + 1, w - 20 - mid, 2,
                ColorUtil.withAlpha(theme.accent(), 0.95f),
                ColorUtil.withAlpha(theme.accent(), 0.12f));
        ctx.fillRect(x + 18, y + h - 3, w - 36, 2, ColorUtil.withAlpha(theme.accent(), 0.7f));

        // Corner ticks
        int tick = 10;
        int accent = ColorUtil.withAlpha(theme.accent(), 0.9f);
        ctx.fillRect(x + 2, y + 2, tick, 2, accent);
        ctx.fillRect(x + 2, y + 2, 2, tick, accent);
        ctx.fillRect(x + w - tick - 2, y + 2, tick, 2, accent);
        ctx.fillRect(x + w - 4, y + 2, 2, tick, accent);
        ctx.fillRect(x + 2, y + h - 4, tick, 2, accent);
        ctx.fillRect(x + 2, y + h - tick - 2, 2, tick, accent);
        ctx.fillRect(x + w - tick - 2, y + h - 4, tick, 2, accent);
        ctx.fillRect(x + w - 4, y + h - tick - 2, 2, tick, accent);
        ctx.setDrawOpacity(1f);
    }

    private void renderPrimary(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                               GameMenuLayout layout, double mouseX, double mouseY, float fade) {
        ctx.setDrawOpacity(fade);
        int x = layout.primaryX();
        int y = layout.primaryY();
        int w = layout.primaryW();
        int h = layout.primaryH();
        boolean hover = hit(mouseX, mouseY, x, y, w, h);

        int fill = hover
                ? ColorUtil.withAlpha(theme.backgroundLight(), 0.72f)
                : ColorUtil.withAlpha(0xFF161618, 0.95f);
        ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_SM, fill);
        // Diamond hatch (subtle)
        for (int i = 0; i < w; i += 8) {
            ctx.fillRect(x + i, y + 2, 1, h - 4, ColorUtil.withAlpha(0xFFFFFFFF, 0.03f));
        }
        ctx.fillGradientHorizontal(x + 2, y, w - 4, 2,
                ColorUtil.withAlpha(theme.accent(), hover ? 1f : 0.75f),
                ColorUtil.withAlpha(theme.accent(), 0.2f));
        if (hover) {
            ctx.fillRoundedBorder(x, y, w, h, PrimeDesign.RADIUS_SM, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.45f), fill);
        }

        String label = adapter.translate("menu.returnToGame", "Back to Game")
                .toUpperCase(java.util.Locale.ROOT);
        float scale = 0.92f;
        int iconW = 8;
        int textW = ctx.smoothTextWidth(label, scale);
        int start = x + (w - (iconW + 6 + textW)) / 2;
        int textY = textTop(ctx, y, h, scale);
        drawPlayIcon(ctx, start, y + h / 2, theme.foreground());
        ctx.drawSmoothText(label, start + iconW + 6, textY, theme.foreground(), scale);
        ctx.setDrawOpacity(1f);
    }

    private void renderGrid(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                            GameMenuLayout layout, double mouseX, double mouseY, float fade) {
        ctx.setDrawOpacity(fade);
        for (int i = 0; i < GRID.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = layout.gridCellX(col);
            int y = layout.gridCellY(row);
            int w = layout.cellW();
            int h = layout.cellH();
            boolean hover = hit(mouseX, mouseY, x, y, w, h);

            int fill = hover
                    ? ColorUtil.withAlpha(theme.backgroundLight(), 0.65f)
                    : ColorUtil.withAlpha(0xFF141416, 0.92f);
            ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_SM, fill);
            ctx.fillRect(x + 2, y, w - 4, 1, ColorUtil.withAlpha(theme.accent(), hover ? 0.85f : 0.35f));
            if (hover) {
                ctx.fillRoundedBorder(x, y, w, h, PrimeDesign.RADIUS_SM, 1,
                        ColorUtil.withAlpha(theme.accent(), 0.4f), fill);
            }

            String label = adapter.translate(GRID_KEYS[i], GRID_FALLBACKS[i])
                    .toUpperCase(java.util.Locale.ROOT);
            float scale = 0.78f;
            int iconColor = theme.accent();
            int textColor = hover ? theme.foreground() : theme.foregroundMuted();
            int textY = textTop(ctx, y, h, scale);
            int iconY = textTop(ctx, y, h, 0.85f);
            ctx.drawSmoothText(GRID_ICONS[i], x + 8, iconY, iconColor, 0.85f);
            ctx.drawSmoothText(label, x + 22, textY, textColor, scale);
        }
        ctx.setDrawOpacity(1f);
    }

    private void renderQuit(RenderContext ctx, Theme theme, MinecraftAdapter adapter,
                            GameMenuLayout layout, double mouseX, double mouseY, float fade) {
        ctx.setDrawOpacity(fade);
        int x = layout.quitX();
        int y = layout.quitY();
        int w = layout.quitW();
        int h = layout.quitH();
        boolean hover = hit(mouseX, mouseY, x, y, w, h);

        int fill = hover
                ? ColorUtil.withAlpha(theme.accentSecondary(), 0.35f)
                : ColorUtil.withAlpha(0xFF121214, 0.95f);
        ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_SM, fill);
        ctx.fillRoundedBorder(x, y, w, h, PrimeDesign.RADIUS_SM, 1,
                ColorUtil.withAlpha(theme.accent(), hover ? 0.7f : 0.28f), fill);

        String label = adapter.translate("menu.returnToMenu", "Save and Quit to Title")
                .toUpperCase(java.util.Locale.ROOT);
        // Multiplayer uses Disconnect â€” prefer that label when connected remotely
        if (adapter.isMultiplayer()) {
            label = adapter.translate("menu.disconnect", "Disconnect")
                    .toUpperCase(java.util.Locale.ROOT);
        }
        float scale = 0.84f;
        int textW = ctx.smoothTextWidth(label, scale);
        int start = x + (w - textW - 14) / 2;
        int textY = textTop(ctx, y, h, scale);
        ctx.drawSmoothText("â»", start, textTop(ctx, y, h, 0.9f), theme.accent(), 0.9f);
        ctx.drawSmoothText(label, start + 14, textY, theme.foreground(), scale);
        ctx.setDrawOpacity(1f);
    }

    private void renderFooter(RenderContext ctx, Theme theme, GameMenuLayout layout, float fade) {
        ctx.setDrawOpacity(fade);
        int y = layout.footerY();
        int h = layout.footerH();
        int w = ctx.screenWidth();
        ctx.fillRect(0, y, w, h, ColorUtil.withAlpha(0xFF080809, 0.94f));
        ctx.fillRect(0, y, w, 1, ColorUtil.withAlpha(theme.accent(), 0.35f));

        // Left brand
        float brandScale = 0.78f;
        PrimeLogo.draw(ctx, 8, y + (h - 16) / 2, 16, 0xFFFFFFFF);
        ctx.drawSmoothText("StellarClient", 28, textTop(ctx, y, h, brandScale),
                theme.accent(), brandScale);

        // Center slogans
        String[] slogans = {
                "âš¡ OPTIMIZED PERFORMANCE",
                "ðŸ›¡ BUILT FOR COMPETITION",
                "âŒ– MADE TO WIN"
        };
        int sloganX = Math.max(120, w / 2 - 160);
        for (int i = 0; i < slogans.length; i++) {
            if (i > 0) {
                ctx.fillRect(sloganX - 6, y + 8, 1, h - 16, ColorUtil.withAlpha(0xFFFFFFFF, 0.12f));
            }
            ctx.drawSmoothText(slogans[i], sloganX, textTop(ctx, y, h, 0.68f),
                    theme.foregroundMuted(), 0.68f);
            sloganX += ctx.smoothTextWidth(slogans[i], 0.68f) + 14;
        }

        // Right CTA
        int ctaW = 150;
        int ctaX = w - ctaW - 8;
        int ctaY = y + 4;
        int ctaH = h - 8;
        ctx.fillRoundedBorder(ctaX, ctaY, ctaW, ctaH, PrimeDesign.RADIUS_SM, 1,
                ColorUtil.withAlpha(theme.accent(), 0.65f),
                ColorUtil.withAlpha(0xFF10080A, 0.9f));
        ctx.drawSmoothText("ENHANCE YOUR GAME.", ctaX + 8, ctaY + 3, theme.foreground(), 0.62f);
        ctx.drawSmoothText("DOMINATE EVERY MATCH.", ctaX + 8, ctaY + 13, theme.accent(), 0.62f);
        ctx.setDrawOpacity(1f);
    }

    private static void drawPlayIcon(RenderContext ctx, int x, int cy, int color) {
        // Simple right-pointing triangle
        ctx.fillRect(x, cy - 4, 1, 8, color);
        ctx.fillRect(x + 1, cy - 3, 1, 6, color);
        ctx.fillRect(x + 2, cy - 2, 1, 4, color);
        ctx.fillRect(x + 3, cy - 1, 1, 2, color);
        ctx.fillRect(x + 4, cy, 1, 1, color);
    }

    /** Top Y for scaled smooth text optically centered in a button/row. */
    private static int textTop(RenderContext ctx, int y, int h, float scale) {
        int glyphH = Math.max(1, Math.round(ctx.fontHeight() * scale));
        return y + (h - glyphH) / 2;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
