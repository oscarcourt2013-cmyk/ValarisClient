package dev.stellarclient.core.gui.menu;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.design.PrimeLogo;
import dev.stellarclient.core.gui.BlurBackdrop;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.clickgui.ClickGuiView;
import dev.stellarclient.core.i18n.PrimeLang;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;
import dev.stellarclient.core.util.Easing;

/**
 * Feather-style ClickGUI hub â€” compact stacked buttons over panorama/blur.
 */
public final class MainMenuRenderer {

    public static final int PANEL_WIDTH = ClickGuiMenuLayout.MENU_W;

    private static final String[] LABEL_KEYS = {
            "prime.gui.main_menu.resume",
            "prime.gui.main_menu.modules",
            "prime.gui.main_menu.hud_editor",
            "prime.gui.main_menu.configurations",
            "prime.gui.main_menu.cosmetics",
            "prime.gui.main_menu.settings"
    };

    private static final String[] LABEL_FALLBACKS = {
            "Resume", "Modules", "HUD Editor", "Configurations", "Cosmetics", "Settings"
    };

    private float particlePhase;

    public void tick(float deltaSeconds) {
        particlePhase += deltaSeconds * 0.4f;
    }

    public void renderBackground(RenderContext ctx, Theme theme, float openFade) {
        if (BlurBackdrop.isActive()) {
            int vignette = Math.round(56 * Easing.easeOutCubic(openFade));
            ctx.fillRect(0, 0, ctx.screenWidth(), ctx.screenHeight(), (vignette << 24));
        }
    }

    public int panelX(int screenWidth) {
        return (screenWidth - PANEL_WIDTH) / 2;
    }

    public int panelY(int screenWidth, int screenHeight, float menuSlide) {
        ClickGuiMenuLayout layout = ClickGuiMenuLayout.compute(screenWidth, screenHeight, LABEL_KEYS.length);
        return layout.menuY() + Math.round(menuSlide);
    }

    public int panelHeight() {
        return 14 + 10 + LABEL_KEYS.length * (ClickGuiMenuLayout.BUTTON_H + ClickGuiMenuLayout.BUTTON_GAP) + 14;
    }

    public void renderPanel(RenderContext ctx, Theme theme, int x, int y,
                          String playerName, String version, double mouseX, double mouseY,
                          float menuSlide) {
        ClickGuiMenuLayout layout = ClickGuiMenuLayout.compute(ctx.screenWidth(), ctx.screenHeight(), LABEL_KEYS.length);
        int slideY = Math.max(-layout.logoY() + 12, Math.round(menuSlide));

        PrimeLogo.draw(ctx, layout.logoX(), layout.logoY() + slideY, layout.logoH(), 0xFFFFFFFF);

        String versionLabel = PrimeLang.get("prime.gui.main_menu.version", "Prime %s", version);
        int versionW = GuiLayout.labelWidth(ctx, versionLabel);
        int versionY = layout.logoY() + slideY + layout.logoH() + 4;
        GuiLayout.label(ctx, versionLabel,
                layout.menuX() + (layout.menuW() - versionW) / 2,
                versionY,
                theme.foregroundMuted());

        for (int i = 0; i < LABEL_KEYS.length; i++) {
            int bx = layout.menuX();
            int by = layout.buttonY(i) + slideY;
            String label = PrimeLang.get(LABEL_KEYS[i], LABEL_FALLBACKS[i]);
            drawCompactButton(ctx, theme, label, bx, by, layout.menuW(), layout.buttonH(),
                    mouseX, mouseY, i == 0);
        }

        String hint = PrimeLang.get("prime.gui.main_menu.hint", "Right Shift  Â·  H = HUD");
        int hintW = GuiLayout.labelWidth(ctx, hint);
        GuiLayout.label(ctx, hint,
                (ctx.screenWidth() - hintW) / 2,
                layout.footerY() + slideY,
                theme.foregroundMuted());
    }

    private void drawCompactButton(RenderContext ctx, Theme theme, String label,
                                   int x, int y, int w, int h,
                                   double mouseX, double mouseY, boolean primary) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int radius = PrimeDesign.RADIUS_SM;

        if (primary) {
            int base = hover ? theme.accentSecondary() : theme.accent();
            ctx.fillRoundedRect(x, y, w, h, radius, base);
            if (hover) {
                ctx.fillGradientVertical(x + 1, y + 1, w - 2, h - 2,
                        ColorUtil.withAlpha(theme.accent(), 0.95f),
                        ColorUtil.withAlpha(theme.accentSecondary(), 0.88f));
            }
        } else {
            int fill = hover
                    ? ColorUtil.withAlpha(theme.backgroundLight(), BlurBackdrop.isActive() ? 0.62f : 0.72f)
                    : ColorUtil.withAlpha(theme.surfaceElevated(), BlurBackdrop.isActive() ? 0.42f : 0.58f);
            ctx.fillRoundedRect(x, y, w, h, radius, fill);
            if (hover) {
                ctx.fillRoundedBorder(x, y, w, h, radius, 1,
                        ColorUtil.withAlpha(theme.accent(), 0.55f), fill);
            }
        }

        int textColor = primary || hover ? theme.foreground() : theme.foregroundMuted();
        int labelW = GuiLayout.labelWidth(ctx, label);
        GuiLayout.label(ctx, label, x + (w - labelW) / 2, y + (h - ctx.fontHeight()) / 2 + 1, textColor);
    }

    public ClickGuiView viewForButton(int index) {
        return switch (index) {
            case 0 -> null;
            case 1 -> ClickGuiView.BROWSE;
            case 2 -> null;
            case 3 -> ClickGuiView.CONFIGURATIONS;
            case 4 -> ClickGuiView.COSMETICS;
            case 5 -> ClickGuiView.SETTINGS;
            default -> ClickGuiView.MAIN_MENU;
        };
    }

    public int hitButton(double mouseX, double mouseY, int screenWidth, int screenHeight, float menuSlide) {
        ClickGuiMenuLayout layout = ClickGuiMenuLayout.compute(screenWidth, screenHeight, LABEL_KEYS.length);
        int slideY = Math.max(-layout.logoY() + 12, Math.round(menuSlide));
        for (int i = 0; i < LABEL_KEYS.length; i++) {
            int by = layout.buttonY(i) + slideY;
            if (mouseX >= layout.menuX() && mouseX < layout.menuX() + layout.menuW()
                    && mouseY >= by && mouseY < by + layout.buttonH()) {
                return i;
            }
        }
        return -1;
    }
}
