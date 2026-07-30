package dev.valerisclient.core.gui.options;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.design.PrimeDesign;
import dev.valerisclient.core.design.PrimeLogo;
import dev.valerisclient.core.gui.GuiLayout;
import dev.valerisclient.core.gui.UiChrome;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.util.ColorUtil;
import dev.valerisclient.core.util.Easing;

import java.util.List;

/**
 * Draws and drives a Valeris-styled options screen from an {@link OptionsPage}.
 *
 * <p>Stateless with respect to the option values themselves -- every row reads
 * through its own supplier each frame, so external changes show up immediately.</p>
 */
public final class OptionsMenuRenderer {

    private static final int SCROLLBAR_W = 4;
    private static final int SLIDER_TRACK_H = 3;

    private float scrollY;
    private float targetScrollY;
    private int draggingSlider = -1;
    private boolean draggingThumb;
    private double dragAnchorY;
    private float dragAnchorScroll;

    /** Reset when the page changes so a new page starts at the top. */
    public void resetScroll() {
        scrollY = 0f;
        targetScrollY = 0f;
        draggingSlider = -1;
        draggingThumb = false;
    }

    public void tick(float deltaSeconds) {
        scrollY = Easing.lerp(scrollY, targetScrollY, deltaSeconds * 14f);
    }

    public void render(RenderContext ctx, Theme theme, OptionsMenuLayout l, OptionsPage page,
                       String backLabel, double mouseX, double mouseY) {
        List<OptionRow> rows = page.rows();

        ctx.fillRect(0, 0, ctx.screenWidth(), ctx.screenHeight(), theme.overlay());
        UiChrome.glassPanel(ctx, theme, l.panelX(), l.panelY(), l.panelW(), l.panelH());

        renderHeader(ctx, theme, l, page);

        int maxScroll = l.maxScroll(rows.size());
        targetScrollY = GuiLayout.clamp(Math.round(targetScrollY), 0, maxScroll);
        if (scrollY > maxScroll) {
            scrollY = maxScroll;
        }

        ctx.pushClip(l.gridX(), l.gridY(), l.gridW(), Math.max(0, l.gridH()));
        for (int i = 0; i < rows.size(); i++) {
            int rx = l.rowX(i);
            int ry = l.gridY() + l.rowYOffset(i) - Math.round(scrollY);
            if (ry + l.rowH() < l.gridY() || ry > l.gridY() + l.gridH()) {
                continue;
            }
            renderRow(ctx, theme, rows.get(i), rx, ry, l.rowW(), l.rowH(), mouseX, mouseY);
        }
        ctx.popClip();

        renderScrollbar(ctx, theme, l, maxScroll, mouseX, mouseY);
        renderFooter(ctx, theme, l, backLabel, mouseX, mouseY);
    }

    private void renderHeader(RenderContext ctx, Theme theme, OptionsMenuLayout l, OptionsPage page) {
        int centerX = l.panelX() + l.panelW() / 2;
        int y = l.headerY();
        if (l.showBranding()) {
            int logoH = 14;
            PrimeLogo.drawCentered(ctx, centerX, y, logoH, 0xFFFFFFFF);
            y += logoH + 3;
        }
        String title = GuiLayout.trimToWidth(ctx, page.title(), l.panelW() - OptionsMenuLayout.PAD * 2);
        GuiLayout.label(ctx, title, centerX - GuiLayout.labelWidth(ctx, title) / 2, y, theme.foreground());
        int lineW = Math.min(90, l.panelW() - OptionsMenuLayout.PAD * 2);
        ctx.fillGradientHorizontal(centerX - lineW / 2, y + ctx.uiFontHeight() + 2, lineW, 1,
                ColorUtil.withAlpha(theme.accent(), 0.1f), theme.accent());
    }

    private void renderRow(RenderContext ctx, Theme theme, OptionRow row, int x, int y, int w, int h,
                           double mouseX, double mouseY) {
        boolean hover = row.enabled() && hit(mouseX, mouseY, x, y, w, h);
        int fill = hover ? theme.backgroundLight() : ColorUtil.withAlpha(theme.surfaceElevated(), 0.55f);
        ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_SM, fill);
        if (hover) {
            ctx.fillRoundedBorder(x, y, w, h, PrimeDesign.RADIUS_SM, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.5f), fill);
        }

        int textY = y + (h - ctx.uiFontHeight()) / 2 + 1;
        int labelColor = row.enabled()
                ? (hover ? theme.foreground() : ColorUtil.withAlpha(theme.foreground(), 0.85f))
                : theme.foregroundMuted();

        switch (row) {
            case OptionRow.Toggle t -> {
                boolean on = t.get().getAsBoolean();
                int tw = PrimeDesign.TOGGLE_WIDTH;
                int th = PrimeDesign.TOGGLE_HEIGHT;
                int tx = x + w - 6 - tw;
                int ty = y + (h - th) / 2;
                label(ctx, t.label(), x, textY, tx - x - 8, labelColor);
                ctx.fillRoundedRect(tx, ty, tw, th, th / 2,
                        on ? theme.success() : ColorUtil.withAlpha(theme.backgroundLight(), 0.95f));
                int knob = th - 4;
                ctx.fillRoundedRect(on ? tx + tw - knob - 2 : tx + 2, ty + 2, knob, knob,
                        knob / 2, 0xFFFFFFFF);
            }
            case OptionRow.Cycle c -> {
                String value = c.display().get();
                int vw = GuiLayout.labelWidth(ctx, value);
                int maxV = Math.max(0, w / 2 - 8);
                String shown = GuiLayout.trimToWidth(ctx, value, maxV);
                vw = GuiLayout.labelWidth(ctx, shown);
                label(ctx, c.label(), x, textY, w - vw - 18, labelColor);
                GuiLayout.label(ctx, shown, x + w - 6 - vw, textY, theme.accent());
            }
            case OptionRow.Slider s -> {
                String value = s.display().get();
                int vw = GuiLayout.labelWidth(ctx, value);
                label(ctx, s.label(), x, y + 2, w - vw - 14, labelColor);
                GuiLayout.label(ctx, value, x + w - 6 - vw, y + 2, theme.accent());
                int barX = x + 6;
                int barW = Math.max(1, w - 12);
                int barY = y + h - 5;
                ctx.fillRoundedRect(barX, barY, barW, SLIDER_TRACK_H, 1,
                        ColorUtil.withAlpha(theme.border(), 0.6f));
                float f = (float) Math.clamp(s.fraction().getAsDouble(), 0d, 1d);
                ctx.fillRoundedRect(barX, barY, Math.max(1, Math.round(barW * f)), SLIDER_TRACK_H, 1,
                        theme.accent());
            }
            case OptionRow.Link k -> {
                String chevron = ">";
                int cw = GuiLayout.labelWidth(ctx, chevron);
                label(ctx, k.label(), x, textY, w - cw - 18, labelColor);
                GuiLayout.label(ctx, chevron, x + w - 6 - cw, textY,
                        hover ? theme.accent() : theme.foregroundMuted());
            }
            case OptionRow.Info info -> {
                String value = info.value().get();
                int vw = GuiLayout.labelWidth(ctx, value);
                label(ctx, info.label(), x, textY, w - vw - 14, labelColor);
                GuiLayout.label(ctx, value, x + w - 6 - vw, textY, theme.foregroundMuted());
            }
        }
    }

    private static void label(RenderContext ctx, String text, int x, int y, int maxW, int color) {
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, text, Math.max(0, maxW)), x + 6, y, color);
    }

    private void renderScrollbar(RenderContext ctx, Theme theme, OptionsMenuLayout l, int maxScroll,
                                double mouseX, double mouseY) {
        if (maxScroll <= 0 || l.gridH() <= 0) {
            return;
        }
        int trackX = l.gridX() + l.gridW() - SCROLLBAR_W;
        ctx.fillRoundedRect(trackX, l.gridY(), SCROLLBAR_W, l.gridH(), SCROLLBAR_W / 2,
                ColorUtil.withAlpha(theme.border(), 0.35f));
        int thumbH = thumbHeight(l, maxScroll);
        int thumbY = l.gridY() + Math.round((l.gridH() - thumbH) * (scrollY / (float) maxScroll));
        boolean hot = draggingThumb || hit(mouseX, mouseY, trackX - 2, l.gridY(),
                SCROLLBAR_W + 4, l.gridH());
        ctx.fillRoundedRect(trackX, thumbY, SCROLLBAR_W, thumbH, SCROLLBAR_W / 2,
                hot ? theme.accent() : ColorUtil.withAlpha(theme.foregroundMuted(), 0.8f));
    }

    private int thumbHeight(OptionsMenuLayout l, int maxScroll) {
        float visible = l.gridH() / (float) (l.gridH() + maxScroll);
        return Math.max(16, Math.round(l.gridH() * visible));
    }

    private void renderFooter(RenderContext ctx, Theme theme, OptionsMenuLayout l, String backLabel,
                              double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, l.footerX(), l.footerY(), l.footerW(), l.footerH());
        UiChrome.button(ctx, theme, l.footerX(), l.footerY(), l.footerW(), l.footerH(), hover, true);
        int lw = GuiLayout.labelWidth(ctx, backLabel);
        GuiLayout.label(ctx, backLabel, l.footerX() + (l.footerW() - lw) / 2,
                l.footerY() + (l.footerH() - ctx.uiFontHeight()) / 2 + 1, theme.foreground());
    }

    // --- input ---

    /** @return {@code true} when the footer (Done / Back) was pressed. */
    public boolean mousePressed(OptionsMenuLayout l, OptionsPage page, double mouseX, double mouseY) {
        if (hit(mouseX, mouseY, l.footerX(), l.footerY(), l.footerW(), l.footerH())) {
            return true;
        }

        List<OptionRow> rows = page.rows();
        int maxScroll = l.maxScroll(rows.size());
        if (maxScroll > 0) {
            int trackX = l.gridX() + l.gridW() - SCROLLBAR_W;
            if (hit(mouseX, mouseY, trackX - 2, l.gridY(), SCROLLBAR_W + 4, l.gridH())) {
                draggingThumb = true;
                dragAnchorY = mouseY;
                dragAnchorScroll = targetScrollY;
                return false;
            }
        }

        for (int i = 0; i < rows.size(); i++) {
            int rx = l.rowX(i);
            int ry = l.gridY() + l.rowYOffset(i) - Math.round(scrollY);
            if (ry + l.rowH() < l.gridY() || ry > l.gridY() + l.gridH()) {
                continue;
            }
            if (!hit(mouseX, mouseY, rx, ry, l.rowW(), l.rowH())) {
                continue;
            }
            OptionRow row = rows.get(i);
            switch (row) {
                case OptionRow.Toggle t -> t.set().accept(!t.get().getAsBoolean());
                case OptionRow.Cycle c -> c.next().run();
                case OptionRow.Slider s -> {
                    draggingSlider = i;
                    applySlider(s, l, rx, mouseX);
                }
                case OptionRow.Link k -> k.action().run();
                case OptionRow.Info ignored -> {
                }
            }
            return false;
        }
        return false;
    }

    public void mouseDragged(OptionsMenuLayout l, OptionsPage page, double mouseX, double mouseY) {
        if (draggingThumb) {
            int maxScroll = l.maxScroll(page.rows().size());
            if (maxScroll <= 0) {
                return;
            }
            int travel = Math.max(1, l.gridH() - thumbHeight(l, maxScroll));
            float delta = (float) (mouseY - dragAnchorY) / travel * maxScroll;
            targetScrollY = GuiLayout.clamp(Math.round(dragAnchorScroll + delta), 0, maxScroll);
            scrollY = targetScrollY;
            return;
        }
        if (draggingSlider >= 0 && draggingSlider < page.rows().size()
                && page.rows().get(draggingSlider) instanceof OptionRow.Slider s) {
            applySlider(s, l, l.rowX(draggingSlider), mouseX);
        }
    }

    public void mouseReleased() {
        draggingSlider = -1;
        draggingThumb = false;
    }

    private static void applySlider(OptionRow.Slider s, OptionsMenuLayout l, int rowX, double mouseX) {
        int barX = rowX + 6;
        int barW = Math.max(1, l.rowW() - 12);
        s.setFraction().accept(Math.clamp((mouseX - barX) / barW, 0d, 1d));
    }

    public boolean mouseScrolled(OptionsMenuLayout l, OptionsPage page, double amount) {
        int maxScroll = l.maxScroll(page.rows().size());
        if (maxScroll <= 0) {
            return false;
        }
        targetScrollY = GuiLayout.clamp(Math.round(targetScrollY - (float) amount * 20f), 0, maxScroll);
        return true;
    }

    /** Keyboard scrolling so the list is reachable without a wheel or trackpad gesture. */
    public boolean keyPressed(OptionsMenuLayout l, OptionsPage page, int glfwKey) {
        int maxScroll = l.maxScroll(page.rows().size());
        if (maxScroll <= 0) {
            return false;
        }
        int step = l.rowH() + OptionsMenuLayout.ROW_GAP;
        Integer target = switch (glfwKey) {
            case 264 -> Math.round(targetScrollY) + step;
            case 265 -> Math.round(targetScrollY) - step;
            case 267 -> Math.round(targetScrollY) + Math.max(step, l.gridH());
            case 266 -> Math.round(targetScrollY) - Math.max(step, l.gridH());
            case 268 -> 0;
            case 269 -> maxScroll;
            default -> null;
        };
        if (target == null) {
            return false;
        }
        targetScrollY = GuiLayout.clamp(target, 0, maxScroll);
        return true;
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
