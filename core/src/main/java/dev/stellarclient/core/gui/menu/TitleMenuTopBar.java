package dev.stellarclient.core.gui.menu;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.discord.DiscordRpcService;
import dev.stellarclient.core.i18n.PrimeLang;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;

/** Feather-style top-right shortcuts on the title screen. */
public final class TitleMenuTopBar {

    public enum Action {
        DISCORD,
        VANILLA,
        SETTINGS,
        PROFILE
    }

    private static final int BTN = 22;
    private static final int GAP = 4;
    private static final int PAD = 10;
    private static final float ICON_SCALE = 0.88f;

    /** Last render layout — hit-testing must match drawn chip width (smooth font). */
    private static volatile Layout lastLayout;

    private TitleMenuTopBar() {
    }

    public static String discordUrl() {
        return "https://discord.com/application-directory/" + DiscordRpcService.APPLICATION_ID;
    }

    public static void render(RenderContext ctx, Theme theme, String playerName, String accountType,
                              double mouseX, double mouseY, float fade) {
        Layout layout = layout(ctx, playerName, accountType);
        lastLayout = layout;
        ctx.setDrawOpacity(fade);

        drawIconButton(ctx, theme, layout.discordX(), layout.y(), "D", mouseX, mouseY);
        drawIconButton(ctx, theme, layout.vanillaX(), layout.y(), "M", mouseX, mouseY);
        drawIconButton(ctx, theme, layout.settingsX(), layout.y(), "⚙", mouseX, mouseY);

        if (layout.profileW() > 0) {
            boolean hover = hit(mouseX, mouseY, layout.profileX(), layout.y(), layout.profileW(), BTN);
            int fill = hover
                    ? ColorUtil.withAlpha(theme.backgroundLight(), 0.58f)
                    : ColorUtil.withAlpha(theme.surfaceElevated(), 0.44f);
            ctx.fillRoundedRect(layout.profileX(), layout.y(), layout.profileW(), BTN,
                    PrimeDesign.RADIUS_SM, fill);
            ctx.fillRoundedBorder(layout.profileX(), layout.y(), layout.profileW(), BTN,
                    PrimeDesign.RADIUS_SM, 1,
                    ColorUtil.withAlpha(theme.accent(), hover ? 0.7f : 0.28f), fill);
            ctx.drawSmoothText(layout.profileLabel(), layout.profileX() + 8,
                    layout.y() + (BTN - ctx.fontHeight()) / 2 + 1, theme.foreground(), 0.88f);
        }

        ctx.setDrawOpacity(1f);
    }

    public static Action hitAction(double mouseX, double mouseY, int screenWidth,
                                   String playerName, String accountType) {
        Layout layout = lastLayout;
        if (layout == null || layout.screenWidth() != screenWidth) {
            layout = layout(screenWidth, playerName, accountType);
        }
        if (layout.profileW() > 0 && hit(mouseX, mouseY, layout.profileX(), layout.y(), layout.profileW(), BTN)) {
            return Action.PROFILE;
        }
        if (hit(mouseX, mouseY, layout.settingsX(), layout.y(), BTN, BTN)) {
            return Action.SETTINGS;
        }
        if (hit(mouseX, mouseY, layout.vanillaX(), layout.y(), BTN, BTN)) {
            return Action.VANILLA;
        }
        if (hit(mouseX, mouseY, layout.discordX(), layout.y(), BTN, BTN)) {
            return Action.DISCORD;
        }
        return null;
    }

    private static void drawIconButton(RenderContext ctx, Theme theme, int x, int y, String icon,
                                       double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, BTN, BTN);
        int fill = hover
                ? ColorUtil.withAlpha(theme.backgroundLight(), 0.58f)
                : ColorUtil.withAlpha(theme.surfaceElevated(), 0.44f);
        ctx.fillRoundedRect(x, y, BTN, BTN, PrimeDesign.RADIUS_SM, fill);
        if (hover) {
            ctx.fillRoundedBorder(x, y, BTN, BTN, PrimeDesign.RADIUS_SM, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.55f), fill);
        }
        int iconW = ctx.smoothTextWidth(icon, ICON_SCALE);
        ctx.drawSmoothText(icon, x + (BTN - iconW) / 2, y + (BTN - ctx.fontHeight()) / 2 + 1,
                hover ? theme.foreground() : theme.foregroundMuted(), ICON_SCALE);
    }

    private static Layout layout(RenderContext ctx, String playerName, String accountType) {
        String label = profileLabel(playerName, accountType);
        int profileW = label.isEmpty() ? 0 : ctx.smoothTextWidth(label, 0.88f) + 16;
        return layout(ctx.screenWidth(), profileW, label);
    }

    private static Layout layout(int screenWidth, String playerName, String accountType) {
        String label = profileLabel(playerName, accountType);
        // Fallback estimate when no render context — prefer lastLayout when available.
        int profileW = label.isEmpty() ? 0 : Math.max(72, (int) (label.length() * 5.6f) + 16);
        return layout(screenWidth, profileW, label);
    }

    private static Layout layout(int screenWidth, int profileW, String label) {
        int profileX = screenWidth - PAD - profileW;
        int settingsX = profileX - (profileW > 0 ? GAP : 0) - BTN;
        int vanillaX = settingsX - GAP - BTN;
        int discordX = vanillaX - GAP - BTN;
        return new Layout(screenWidth, discordX, vanillaX, settingsX, profileX, profileW, PAD, label);
    }

    private static String profileLabel(String playerName, String accountType) {
        String suffix = accountTypeSuffix(accountType);
        if (playerName == null || playerName.isBlank()) {
            String base = PrimeLang.get("prime.gui.account.chip", "⇄ Account");
            return suffix.isEmpty() ? base : base + suffix;
        }
        String trimmed = playerName.trim();
        if (trimmed.length() > 12) {
            return "⇄ " + trimmed.substring(0, 11) + "…" + suffix;
        }
        return "⇄ " + trimmed + suffix;
    }

    private static String accountTypeSuffix(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return "";
        }
        return switch (accountType.toLowerCase(java.util.Locale.ROOT)) {
            case "microsoft" -> " · MS";
            case "offline" -> " · OFF";
            default -> "";
        };
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private record Layout(int screenWidth, int discordX, int vanillaX, int settingsX, int profileX,
                          int profileW, int y, String profileLabel) {
    }
}
