package dev.valarisclient.core.gui.menu;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.design.ValarisDesign;
import dev.valarisclient.core.design.ValarisLogo;
import dev.valarisclient.core.i18n.ValarisLang;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.util.ColorUtil;
import dev.valarisclient.core.util.Easing;

/** Compact title menu — compact stack over visible panorama. */
public final class TitleMenuRenderer {

    private static final int MENU_BUTTON_COUNT = 4;
    private static final TitleMenuAction[] ACTIONS = TitleMenuAction.values();
    private static final String[] LABEL_KEYS = {
            "valaris.gui.title.singleplayer",
            "valaris.gui.title.multiplayer",
            "valaris.gui.title.valaris_client",
            "valaris.gui.title.options"
    };
    private static final String[] LABEL_FALLBACKS = {
            "Singleplayer", "Multiplayer", "ValarisClient", "Options"
    };

    public void tick(float deltaSeconds) {
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY,
                       MinecraftAdapter adapter, float fade) {
        float eased = Easing.easeOutCubic(fade);
        TitleMenuLayout layout = TitleMenuLayout.compute(ctx.screenWidth(), ctx.screenHeight(), MENU_BUTTON_COUNT);

        renderVignette(ctx, eased);
        TitleMenuTopBar.render(ctx, theme, adapter.playerName(), adapter.sessionAccountType(),
                mouseX, mouseY, eased);
        renderBranding(ctx, theme, layout, eased);
        renderButtons(ctx, theme, layout, mouseX, mouseY, eased);
        renderQuitLink(ctx, theme, layout, mouseX, mouseY, eased);
        renderFooter(ctx, theme, adapter.minecraftVersion(), ValarisDesign.VERSION, layout, eased);
    }

    /** Edge vignette only — keeps the panorama visible in the center. */
    private void renderVignette(RenderContext ctx, float fade) {
        int w = ctx.screenWidth();
        int h = ctx.screenHeight();
        int edge = Math.max(48, h / 5);
        int alpha = Math.round(0x38 * fade);
        if (alpha <= 0) {
            return;
        }
        int top = alpha << 24;
        ctx.fillGradientVertical(0, 0, w, edge, top, 0);
        ctx.fillGradientVertical(0, h - edge, w, edge, 0, top);
    }

    private void renderBranding(RenderContext ctx, Theme theme, TitleMenuLayout layout, float fade) {
        ctx.setDrawOpacity(fade);
        ValarisLogo.draw(ctx, layout.logoX(), layout.logoY(), layout.logoH(), 0xFFFFFFFF);
        ctx.setDrawOpacity(1f);
    }

    private void renderButtons(RenderContext ctx, Theme theme, TitleMenuLayout layout,
                               double mouseX, double mouseY, float fade) {
        ctx.setDrawOpacity(fade);
        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            String label = ValarisLang.get(LABEL_KEYS[i], LABEL_FALLBACKS[i]);
            drawMenuButton(ctx, theme, label, layout.buttonX(), layout.buttonTop(i),
                    layout.buttonW(), layout.buttonH(), mouseX, mouseY, i == 2);
        }
        ctx.setDrawOpacity(1f);
    }

    private void drawMenuButton(RenderContext ctx, Theme theme, String label,
                                int x, int y, int w, int h, double mouseX, double mouseY, boolean featured) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        int radius = ValarisDesign.RADIUS_SM;

        if (featured) {
            ctx.fillRoundedRect(x, y, w, h, radius, theme.accent());
            if (hover) {
                ctx.fillGradientVertical(x + 1, y + 1, w - 2, h - 2,
                        ColorUtil.withAlpha(theme.accent(), 0.98f),
                        ColorUtil.withAlpha(theme.accentSecondary(), 0.9f));
            }
        } else {
            int fill = hover
                    ? ColorUtil.withAlpha(theme.backgroundLight(), 0.52f)
                    : ColorUtil.withAlpha(theme.surfaceElevated(), 0.38f);
            ctx.fillRoundedRect(x, y, w, h, radius, fill);
            if (hover) {
                ctx.fillRoundedBorder(x, y, w, h, radius, 1,
                        ColorUtil.withAlpha(theme.accent(), 0.5f), fill);
            }
        }

        int textColor = featured || hover ? theme.foreground() : theme.foregroundMuted();
        float scale = 0.92f;
        int textW = ctx.smoothTextWidth(label, scale);
        ctx.drawSmoothText(label, x + (w - textW) / 2, y + (h - ctx.fontHeight()) / 2 + 1, textColor, scale);
    }

    private void renderQuitLink(RenderContext ctx, Theme theme, TitleMenuLayout layout,
                                double mouseX, double mouseY, float fade) {
        String label = ValarisLang.get("valaris.gui.title.quit", "Quit Game");
        float scale = 0.88f;
        int textW = ctx.smoothTextWidth(label, scale);
        int x = (ctx.screenWidth() - textW) / 2;
        int y = layout.quitY();
        boolean hover = mouseX >= x - 4 && mouseX < x + textW + 4
                && mouseY >= y - 2 && mouseY < y + ctx.fontHeight() + 2;

        ctx.setDrawOpacity(fade);
        ctx.drawSmoothText(label, x, y, hover ? theme.accent() : theme.foregroundMuted(), scale);
        ctx.setDrawOpacity(1f);
    }

    private void renderFooter(RenderContext ctx, Theme theme, String minecraftVersion,
                              String clientVersion, TitleMenuLayout layout, float fade) {
        ctx.setDrawOpacity(fade * 0.75f);
        String footer = ValarisLang.get("valaris.gui.title.footer", "ValarisClient %1$s  ·  Minecraft %2$s",
                clientVersion, minecraftVersion);
        float scale = 0.78f;
        int footerW = ctx.smoothTextWidth(footer, scale);
        ctx.drawSmoothText(footer, (ctx.screenWidth() - footerW) / 2, layout.footerY(),
                theme.foregroundMuted(), scale);
        ctx.setDrawOpacity(1f);
    }

    public TitleMenuAction hitAction(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        TitleMenuLayout layout = TitleMenuLayout.compute(screenWidth, screenHeight, MENU_BUTTON_COUNT);

        int quitW = 72;
        int quitX = (screenWidth - quitW) / 2;
        if (mouseX >= quitX && mouseX < quitX + quitW
                && mouseY >= layout.quitY() - 2 && mouseY < layout.quitY() + 14) {
            return TitleMenuAction.QUIT;
        }

        for (int i = 0; i < MENU_BUTTON_COUNT; i++) {
            int y = layout.buttonTop(i);
            if (mouseX >= layout.buttonX() && mouseX < layout.buttonX() + layout.buttonW()
                    && mouseY >= y && mouseY < y + layout.buttonH()) {
                return ACTIONS[i];
            }
        }
        return null;
    }
}
