package dev.valarisclient.core.gui.menu;

import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.design.ValarisLogo;
import dev.valarisclient.core.gui.UiChrome;
import dev.valarisclient.core.i18n.ValarisLang;
import dev.valarisclient.core.theme.Theme;

/**
 * Interactive first-run wizard inside the ClickGUI.
 *
 * <p>Steps: theme → profile preset → keybinds → finish.</p>
 */
public final class OnboardingScreen {

    // Kept within Minecraft's smallest effective GUI canvas (320x240).
    public static final int PANEL_W = 300;
    public static final int PANEL_H = 196;

    private static final int INSET = 12;
    private static final int ROW_H = 22;
    private static final int CHOICE_GAP = 4;

    /** Theme id, translation key, fallback label. */
    private static final String[][] THEME_CHOICES = {
        {"valaris-crimson", "valaris.gui.settings.theme.crimson", "Crimson"},
        {"valaris-midnight", "valaris.gui.settings.theme.midnight", "Midnight"},
        {"valaris-aurora", "valaris.gui.settings.theme.aurora", "Aurora"},
        {"valaris-obsidian", "valaris.gui.settings.theme.obsidian", "Obsidian"},
        {"valaris-ember", "valaris.gui.settings.theme.ember", "Ember"}
    };

    /** Profile id, translation key, fallback label. */
    private static final String[][] PROFILE_CHOICES = {
        {"default", "valaris.gui.onboarding.profile.balanced", "Balanced"},
        {"pvp", "valaris.gui.onboarding.profile.pvp", "PvP"},
        {"survival", "valaris.gui.onboarding.profile.survival", "Survival"}
    };

    private OnboardingScreen() {
    }

    /**
     * Width of one choice in a row of {@code count}, derived from the panel so a
     * row can never run past the inset. Hardcoding these overflowed the panel.
     */
    private static int choiceWidth(int count) {
        return (PANEL_W - INSET * 2 - CHOICE_GAP * (count - 1)) / count;
    }

    private static int choiceX(int panelX, int index, int count) {
        return panelX + INSET + index * (choiceWidth(count) + CHOICE_GAP);
    }

    public static void render(RenderContext ctx, Theme theme, OnboardingManager onboarding,
                              int screenW, int screenH, float menuSlide, double mouseX, double mouseY) {
        int x = (screenW - PANEL_W) / 2;
        int y = (screenH - PANEL_H) / 2 + Math.round(menuSlide);
        UiChrome.glassPanel(ctx, theme, x, y, PANEL_W, PANEL_H);
        ValarisLogo.drawCentered(ctx, x + PANEL_W / 2, y + 10, 18, 0xFFFFFFFF);

        int step = onboarding.step();
        switch (step) {
            case 0 -> renderThemeStep(ctx, theme, onboarding, x, y, mouseX, mouseY);
            case 1 -> renderProfileStep(ctx, theme, onboarding, x, y, mouseX, mouseY);
            case 2 -> renderKeybindStep(ctx, theme, x, y, mouseX, mouseY);
            default -> renderFinishStep(ctx, theme, x, y, mouseX, mouseY);
        }

        int displayStep = Math.min(step + 1, 4);
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.step_footer", "Step %1$d/4  ·  Esc = skip", displayStep),
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
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.theme.title", "Choose your theme"),
                x + INSET, y + 36, theme.accent(), true);
        int themeWidth = choiceWidth(THEME_CHOICES.length);
        for (int i = 0; i < THEME_CHOICES.length; i++) {
            String[] choice = THEME_CHOICES[i];
            drawChoice(ctx, theme, choiceX(x, i, THEME_CHOICES.length), y + 54, themeWidth,
                    ValarisLang.get(choice[1], choice[2]),
                    choice[0].equals(onboarding.chosenTheme()), mouseX, mouseY);
        }
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.theme.hint", "Click an option then continue"),
                x + INSET, y + 86, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + INSET, y + 112, PANEL_W - INSET * 2,
                ValarisLang.get("valaris.gui.onboarding.theme.continue", "Continue"), mouseX, mouseY);
    }

    private static void renderProfileStep(RenderContext ctx, Theme theme, OnboardingManager onboarding,
                                          int x, int y, double mouseX, double mouseY) {
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.profile.title", "Module profile"),
                x + INSET, y + 36, theme.accent(), true);
        int profileWidth = choiceWidth(PROFILE_CHOICES.length);
        for (int i = 0; i < PROFILE_CHOICES.length; i++) {
            String[] choice = PROFILE_CHOICES[i];
            drawChoice(ctx, theme, choiceX(x, i, PROFILE_CHOICES.length), y + 56, profileWidth,
                    ValarisLang.get(choice[1], choice[2]),
                    choice[0].equals(onboarding.chosenProfile()), mouseX, mouseY);
        }
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.profile.hint", "FPS, coords, crosshair, Discord RPC included"),
                x + INSET, y + 88, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + INSET, y + 112, PANEL_W - INSET * 2,
                ValarisLang.get("valaris.gui.onboarding.theme.continue", "Continue"), mouseX, mouseY);
    }

    private static void renderKeybindStep(RenderContext ctx, Theme theme, int x, int y,
                                          double mouseX, double mouseY) {
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.keybinds.title", "Essential shortcuts"),
                x + 12, y + 36, theme.accent(), true);
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.keybinds.menu",
                        "Right Shift  →  Valaris Menu (modules, settings)"),
                x + 12, y + 56, theme.foreground(), true);
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.keybinds.hud", "H  →  HUD Editor (move elements)"),
                x + 12, y + 72, theme.foreground(), true);
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.keybinds.zoom", "C  →  Zoom (Zoom module, hold)"),
                x + 12, y + 88, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 112, PANEL_W - 24,
                ValarisLang.get("valaris.gui.onboarding.keybinds.got_it", "Got it!"), mouseX, mouseY);
    }

    private static void renderFinishStep(RenderContext ctx, Theme theme, int x, int y,
                                         double mouseX, double mouseY) {
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.finish.title", "You're all set!"),
                x + 12, y + 36, theme.accent(), true);
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.finish.line1",
                        "Your HUD, crosshair and Discord RPC are ready."),
                x + 12, y + 56, theme.foreground(), true);
        ctx.drawText(ValarisLang.get("valaris.gui.onboarding.finish.line2",
                        "Explore Modules in the menu to customize everything."),
                x + 12, y + 72, theme.foregroundMuted(), true);
        drawPrimary(ctx, theme, x + 12, y + 112, PANEL_W - 24,
                ValarisLang.get("valaris.gui.onboarding.finish.enter", "Enter ValarisClient"), mouseX, mouseY);
    }

    private static boolean handleThemeClick(OnboardingManager onboarding, double mx, double my, int x, int y) {
        int width = choiceWidth(THEME_CHOICES.length);
        for (int i = 0; i < THEME_CHOICES.length; i++) {
            if (hit(mx, my, choiceX(x, i, THEME_CHOICES.length), y + 54, width, ROW_H)) {
                onboarding.setChosenTheme(THEME_CHOICES[i][0]);
                return true;
            }
        }
        if (hit(mx, my, x + INSET, y + 112, PANEL_W - INSET * 2, ROW_H)) {
            onboarding.nextStep();
            return true;
        }
        return true;
    }

    private static boolean handleProfileClick(OnboardingManager onboarding, double mx, double my, int x, int y) {
        int width = choiceWidth(PROFILE_CHOICES.length);
        for (int i = 0; i < PROFILE_CHOICES.length; i++) {
            if (hit(mx, my, choiceX(x, i, PROFILE_CHOICES.length), y + 56, width, ROW_H)) {
                onboarding.setChosenProfile(PROFILE_CHOICES[i][0]);
                return true;
            }
        }
        if (hit(mx, my, x + INSET, y + 112, PANEL_W - INSET * 2, ROW_H)) {
            onboarding.nextStep();
            return true;
        }
        return true;
    }

    private static void drawChoice(RenderContext ctx, Theme theme, int x, int y, int w, String label,
                                   boolean selected, double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, w, ROW_H);
        UiChrome.cardLite(ctx, theme, x, y, w, ROW_H, selected || hover);
        if (selected) {
            ctx.fillRect(x + 2, y + ROW_H - 2, w - 4, 1, theme.accent());
        }
        // Centred, not left-inset: these cards are sized from the panel and can be narrow.
        int textX = x + Math.max(2, (w - ctx.textWidth(label)) / 2);
        ctx.drawText(label, textX, y + 7, selected ? theme.accent() : theme.foreground(), true);
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
