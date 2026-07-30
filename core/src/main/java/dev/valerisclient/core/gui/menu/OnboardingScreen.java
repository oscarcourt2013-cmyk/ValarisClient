package dev.valerisclient.core.gui.menu;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.design.ValerisLogo;
import dev.valerisclient.core.gui.UiChrome;
import dev.valerisclient.core.i18n.ValerisLang;
import dev.valerisclient.core.theme.Theme;

/**
 * Interactive first-run wizard inside the ClickGUI.
 *
 * <p>Steps: theme → profile preset → keybinds → finish.</p>
 */
public final class OnboardingScreen {

    // Kept within Minecraft's smallest effective GUI canvas (320x240).
    public static final int PANEL_W = 300;
    public static final int PANEL_H = 196;

    private OnboardingScreen() {
    }

    public static void render(RenderContext ctx, Theme theme, OnboardingManager onboarding,
                              int screenW, int screenH, float menuSlide, double mouseX, double mouseY) {
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - PANEL_H) / 2 + Math.round(menuSlide);
        UiChrome.glassPanel(ctx, theme, x, y, PANEL_W, PANEL_H);
        ValerisLogo.drawCentered(ctx, x + PANEL_W / 2, y + 10, 18, 0xFFFFFFFF);

        int step = onboarding.step();
        switch (step) {
            case 0 -> renderThemeStep(ctx, theme, onboarding, x, y, mouseX, mouseY);
            case 1 -> renderProfileStep(ctx, theme, onboarding, x, y, mouseX, mouseY);
            case 2 -> renderKeybindStep(ctx, theme, x, y, mouseX, mouseY);
            default -> renderFinishStep(ctx, theme, x, y, mouseX, mouseY);
        }

        int displayStep = Math.min(step + 1, 4);
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.step_footer", "Step %1$d/4  ·  Esc = skip", displayStep),
                x + 12, y + PANEL_H - 14, theme.foregroundMuted(), true);
    }

    public static boolean mousePressed(OnboardingManager onboarding, double mx, double my,
                                       int screenW, int screenH, float menuSlide, int button) {
        if (button != 0) {
            return false;
        }
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - PANEL_H) / 2 + Math.round(menuSlide);
        if (mx < x || mx >= x + PANEL_W || my < y || my >= y + PANEL_H) {
            return false;
        }
        return switch (onboarding.step()) {
            case 0 -> handleThemeClick(onboarding, mx, my, x, y);
            case 1 -> handleProfileClick(onboarding, mx, my, x, y);
            case 2, 3 -> {
                onboarding.nextStep();
                yield true;
            }
            default -> {
                onboarding.nextStep();
                yield true;
            }
        };
    }

    private static void renderThemeStep(RenderContext ctx, Theme theme, OnboardingManager onboarding,
                                        int x, int y, double mouseX, double mouseY) {
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.theme.title", "Choose your theme"),
                x + 12, y + 36, theme.accent(), true);
        drawChoice(ctx, theme, x + 12, y + 54, 60,
                ValerisLang.get("valeris.gui.settings.theme.crimson", "Crimson"),
                "prime-crimson".equals(onboarding.chosenTheme()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 76, y + 54, 60,
                ValerisLang.get("valeris.gui.settings.theme.midnight", "Midnight"),
                "prime-midnight".equals(onboarding.chosenTheme()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 140, y + 54, 60,
                ValerisLang.get("valeris.gui.settings.theme.aurora", "Aurora"),
                "prime-aurora".equals(onboarding.chosenTheme()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 204, y + 54, 60,
                ValerisLang.get("valeris.gui.settings.theme.obsidian", "Obsidian"),
                "prime-obsidian".equals(onboarding.chosenTheme()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 268, y + 54, 60,
                ValerisLang.get("valeris.gui.settings.theme.ember", "Ember"),
                "prime-ember".equals(onboarding.chosenTheme()), mouseX, mouseY);
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.theme.hint", "Click an option then continue"),
                x + 12, y + 86, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 112, PANEL_W - 24,
                ValerisLang.get("valeris.gui.onboarding.theme.continue", "Continue"), mouseX, mouseY);
    }

    private static void renderProfileStep(RenderContext ctx, Theme theme, OnboardingManager onboarding,
                                          int x, int y, double mouseX, double mouseY) {
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.profile.title", "Module profile"),
                x + 12, y + 36, theme.accent(), true);
        drawChoice(ctx, theme, x + 12, y + 56, 100,
                ValerisLang.get("valeris.gui.onboarding.profile.balanced", "Balanced"),
                "default".equals(onboarding.chosenProfile()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 120, y + 56, 100,
                ValerisLang.get("valeris.gui.onboarding.profile.pvp", "PvP"),
                "pvp".equals(onboarding.chosenProfile()), mouseX, mouseY);
        drawChoice(ctx, theme, x + 228, y + 56, 100,
                ValerisLang.get("valeris.gui.onboarding.profile.survival", "Survival"),
                "survival".equals(onboarding.chosenProfile()), mouseX, mouseY);
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.profile.hint", "FPS, coords, crosshair, Discord RPC included"),
                x + 12, y + 88, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 112, PANEL_W - 24,
                ValerisLang.get("valeris.gui.onboarding.theme.continue", "Continue"), mouseX, mouseY);
    }

    private static void renderKeybindStep(RenderContext ctx, Theme theme, int x, int y,
                                          double mouseX, double mouseY) {
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.keybinds.title", "Essential shortcuts"),
                x + 12, y + 36, theme.accent(), true);
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.keybinds.menu",
                        "Right Shift  →  Valeris Menu (modules, settings)"),
                x + 12, y + 56, theme.foreground(), true);
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.keybinds.hud", "H  →  HUD Editor (move elements)"),
                x + 12, y + 72, theme.foreground(), true);
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.keybinds.zoom", "C  →  Zoom (Zoom module, hold)"),
                x + 12, y + 88, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 112, PANEL_W - 24,
                ValerisLang.get("valeris.gui.onboarding.keybinds.got_it", "Got it!"), mouseX, mouseY);
    }

    private static void renderFinishStep(RenderContext ctx, Theme theme, int x, int y,
                                         double mouseX, double mouseY) {
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.finish.title", "You're all set!"),
                x + 12, y + 36, theme.accent(), true);
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.finish.line1",
                        "Your HUD, crosshair and Discord RPC are ready."),
                x + 12, y + 56, theme.foreground(), true);
        ctx.drawText(ValerisLang.get("valeris.gui.onboarding.finish.line2",
                        "Explore Modules in the menu to customize everything."),
                x + 12, y + 72, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 112, PANEL_W - 24,
                ValerisLang.get("valeris.gui.onboarding.finish.enter", "Enter ValerisClient"), mouseX, mouseY);
    }

    private static boolean handleThemeClick(OnboardingManager onboarding, double mx, double my, int x, int y) {
        if (hit(mx, my, x + 12, y + 54, 60, 22)) {
            onboarding.setChosenTheme("prime-crimson");
            return true;
        }
        if (hit(mx, my, x + 76, y + 54, 60, 22)) {
            onboarding.setChosenTheme("prime-midnight");
            return true;
        }
        if (hit(mx, my, x + 140, y + 54, 60, 22)) {
            onboarding.setChosenTheme("prime-aurora");
            return true;
        }
        if (hit(mx, my, x + 204, y + 54, 60, 22)) {
            onboarding.setChosenTheme("prime-obsidian");
            return true;
        }
        if (hit(mx, my, x + 268, y + 54, 60, 22)) {
            onboarding.setChosenTheme("prime-ember");
            return true;
        }
        if (hit(mx, my, x + 12, y + 112, PANEL_W - 24, 22)) {
            onboarding.nextStep();
            return true;
        }
        return true;
    }

    private static boolean handleProfileClick(OnboardingManager onboarding, double mx, double my, int x, int y) {
        if (hit(mx, my, x + 12, y + 56, 100, 22)) {
            onboarding.setChosenProfile("default");
            return true;
        }
        if (hit(mx, my, x + 120, y + 56, 100, 22)) {
            onboarding.setChosenProfile("pvp");
            return true;
        }
        if (hit(mx, my, x + 228, y + 56, 100, 22)) {
            onboarding.setChosenProfile("survival");
            return true;
        }
        if (hit(mx, my, x + 12, y + 112, PANEL_W - 24, 22)) {
            onboarding.nextStep();
            return true;
        }
        return true;
    }

    private static void drawChoice(RenderContext ctx, Theme theme, int x, int y, int w, String label,
                                   boolean selected, double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, w, 22);
        UiChrome.cardLite(ctx, theme, x, y, w, 22, selected || hover);
        if (selected) {
            ctx.fillRect(x + 2, y + 20, w - 4, 1, theme.accent());
        }
        ctx.drawText(label, x + 6, y + 7, selected ? theme.accent() : theme.foreground(), true);
    }

    private static void drawPrimary(RenderContext ctx, Theme theme, int x, int y, int w, String label,
                                    double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, w, 22);
        UiChrome.button(ctx, theme, x, y, w, 22, hover, true);
        int textColor = 0xFFFFFFFF;
        ctx.drawText(label, x + (w - ctx.textWidth(label)) / 2, y + 7, textColor, true);
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
