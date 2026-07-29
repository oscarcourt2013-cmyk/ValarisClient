package dev.stellarclient.core.hud.editor;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD editor chrome: top toolbar, element list (left), properties panel (right)
 * and the hint bar. Geometry is computed each frame in {@link #render} and
 * reused for input hit-testing between frames.
 */
final class HudEditorUi {

    private record Rect(int x, int y, int w, int h) {
        static final Rect EMPTY = new Rect(0, 0, 0, 0);

        boolean contains(double px, double py) {
            return w > 0 && h > 0 && px >= x && px < x + w && py >= y && py < y + h;
        }
    }

    private enum Slider {
        NONE, SCALE, OPACITY, ROTATION
    }

    private static final int PANEL_TOP = 34;
    private static final int LIST_WIDTH = 108;
    private static final int BOTTOM_DOCK_HEIGHT = 74;
    private static final int HINT_RESERVE = 14;
    private static final int ROW_HEIGHT = 13;
    private static final int SWATCH = 12;

    private final HudEditor editor;

    // Toolbar
    private Rect toolbar = Rect.EMPTY;
    private Rect btnGrid = Rect.EMPTY;
    private Rect btnSnap = Rect.EMPTY;
    private Rect btnUndo = Rect.EMPTY;
    private Rect btnRedo = Rect.EMPTY;
    private Rect btnResetAll = Rect.EMPTY;

    // Element list
    private Rect listPanel = Rect.EMPTY;
    private Rect listView = Rect.EMPTY;
    private final List<HudElement> listElements = new ArrayList<>();
    private float listScroll;
    private int listContentHeight;

    // Properties panel
    private Rect propsPanel = Rect.EMPTY;
    private Rect btnVisibility = Rect.EMPTY;
    private Rect trackScale = Rect.EMPTY;
    private Rect trackOpacity = Rect.EMPTY;
    private Rect trackRotation = Rect.EMPTY;
    private Rect swatchRow = Rect.EMPTY;
    private Rect btnCenterX = Rect.EMPTY;
    private Rect btnCenterY = Rect.EMPTY;
    private Rect btnReset = Rect.EMPTY;

    private Rect hintBar = Rect.EMPTY;

    private Slider activeSlider = Slider.NONE;

    HudEditorUi(HudEditor editor) {
        this.editor = editor;
    }

    boolean isOverUi(double mouseX, double mouseY) {
        return toolbar.contains(mouseX, mouseY)
                || listRowIndexAt(mouseX, mouseY) >= 0
                || propsInteractiveHit(mouseX, mouseY);
    }

    /** Bottom dock + toolbar only; side panels never block canvas drags. */
    boolean blocksCanvasDrag(double mouseX, double mouseY) {
        return toolbar.contains(mouseX, mouseY)
                || listRowIndexAt(mouseX, mouseY) >= 0
                || propsInteractiveHit(mouseX, mouseY);
    }

    int bottomDockTop(int screenHeight) {
        return screenHeight - HINT_RESERVE - BOTTOM_DOCK_HEIGHT - 4;
    }

    /** Empty glass chrome on the side panels â€” clicks pass through without deselecting. */
    boolean isPanelBackdrop(double mouseX, double mouseY) {
        if (listPanel.contains(mouseX, mouseY) && listRowIndexAt(mouseX, mouseY) < 0) {
            return true;
        }
        return editor.selected() != null
                && propsPanel.contains(mouseX, mouseY)
                && !propsInteractiveHit(mouseX, mouseY);
    }

    HudElement hoveredListElement(double mouseX, double mouseY) {
        int index = listRowIndexAt(mouseX, mouseY);
        return index >= 0 ? listElements.get(index) : null;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    boolean mousePressed(double mouseX, double mouseY) {
        if (toolbar.contains(mouseX, mouseY)) {
            if (btnGrid.contains(mouseX, mouseY)) {
                editor.toggleGrid();
            } else if (btnSnap.contains(mouseX, mouseY)) {
                editor.toggleSnap();
            } else if (btnUndo.contains(mouseX, mouseY)) {
                editor.undo();
            } else if (btnRedo.contains(mouseX, mouseY)) {
                editor.redo();
            } else if (btnResetAll.contains(mouseX, mouseY)) {
                editor.resetAll();
            }
            return true;
        }
        if (listPanel.contains(mouseX, mouseY)) {
            int index = listRowIndexAt(mouseX, mouseY);
            if (index >= 0) {
                HudElement element = listElements.get(index);
                if (mouseX >= listView.x() + listView.w() - 14) {
                    editor.toggleVisibility(element);
                } else {
                    editor.select(element);
                }
                return true;
            }
            return false;
        }
        if (editor.selected() != null && propsInteractiveHit(mouseX, mouseY)) {
            HudElement selected = editor.selected();
            if (btnVisibility.contains(mouseX, mouseY)) {
                editor.toggleVisibility(selected);
            } else if (trackHit(trackScale, mouseX, mouseY)) {
                activeSlider = Slider.SCALE;
                editor.beginGesture();
                applySlider(mouseX);
            } else if (trackHit(trackOpacity, mouseX, mouseY)) {
                activeSlider = Slider.OPACITY;
                editor.beginGesture();
                applySlider(mouseX);
            } else if (trackHit(trackRotation, mouseX, mouseY)) {
                activeSlider = Slider.ROTATION;
                editor.beginGesture();
                applySlider(mouseX);
            } else if (swatchRow.contains(mouseX, mouseY)) {
                int index = (int) ((mouseX - swatchRow.x()) / (SWATCH + 3));
                if (index >= 0 && index < HudEditor.TINT_PRESETS.length
                        && mouseX - swatchRow.x() - index * (SWATCH + 3) < SWATCH) {
                    editor.setSelectedTint(HudEditor.TINT_PRESETS[index]);
                }
            } else if (btnCenterX.contains(mouseX, mouseY)) {
                editor.centerSelectedX();
            } else if (btnCenterY.contains(mouseX, mouseY)) {
                editor.centerSelectedY();
            } else if (btnReset.contains(mouseX, mouseY)) {
                editor.resetSelected();
            }
            return true;
        }
        return false;
    }

    private boolean propsInteractiveHit(double mouseX, double mouseY) {
        if (editor.selected() == null || !propsPanel.contains(mouseX, mouseY)) {
            return false;
        }
        return btnVisibility.contains(mouseX, mouseY)
                || trackHit(trackScale, mouseX, mouseY)
                || trackHit(trackOpacity, mouseX, mouseY)
                || trackHit(trackRotation, mouseX, mouseY)
                || swatchRow.contains(mouseX, mouseY)
                || btnCenterX.contains(mouseX, mouseY)
                || btnCenterY.contains(mouseX, mouseY)
                || btnReset.contains(mouseX, mouseY);
    }

    /** Widens a 4px slider track to a comfortable click target. */
    private static boolean trackHit(Rect track, double mouseX, double mouseY) {
        return track.w() > 0
                && mouseX >= track.x() - 2 && mouseX < track.x() + track.w() + 2
                && mouseY >= track.y() - 4 && mouseY < track.y() + track.h() + 4;
    }

    boolean mouseDragged(double mouseX, double mouseY) {
        if (activeSlider == Slider.NONE) {
            return false;
        }
        applySlider(mouseX);
        return true;
    }

    void mouseReleased() {
        activeSlider = Slider.NONE;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (listView.contains(mouseX, mouseY)) {
            int maxScroll = Math.max(0, listContentHeight - listView.h());
            if (maxScroll > 0) {
                listScroll = HudEditor.clampSafe((float) (listScroll - scrollDelta * ROW_HEIGHT * 2), 0, maxScroll);
                return true;
            }
        }
        return toolbar.contains(mouseX, mouseY) || propsInteractiveHit(mouseX, mouseY);
    }

    private void applySlider(double mouseX) {
        HudElement selected = editor.selected();
        if (selected == null) {
            activeSlider = Slider.NONE;
            return;
        }
        Rect track = switch (activeSlider) {
            case SCALE -> trackScale;
            case OPACITY -> trackOpacity;
            case ROTATION -> trackRotation;
            case NONE -> Rect.EMPTY;
        };
        if (track.w() <= 0) {
            return;
        }
        editor.markMutated();
        float t = HudEditor.clampSafe((float) (mouseX - track.x()) / track.w(), 0f, 1f);
        switch (activeSlider) {
            case SCALE -> selected.setScale(HudElement.MIN_SCALE
                    + t * (HudElement.MAX_SCALE - HudElement.MIN_SCALE));
            case OPACITY -> selected.setOpacity(HudElement.MIN_OPACITY
                    + t * (HudElement.MAX_OPACITY - HudElement.MIN_OPACITY));
            case ROTATION -> selected.setRotation(snapRotation(-180f + t * 360f));
            case NONE -> {
            }
        }
    }

    /** Rotation slider sticks to the useful angles. */
    private static float snapRotation(float degrees) {
        for (int snap = -180; snap <= 180; snap += 45) {
            if (Math.abs(degrees - snap) <= 3f) {
                return snap;
            }
        }
        return degrees;
    }

    private static float normalizedRotation(HudElement element) {
        float r = element.rotation() % 360f;
        if (r > 180f) {
            r -= 360f;
        }
        if (r < -180f) {
            r += 360f;
        }
        return r;
    }

    int listRowIndexAt(double mouseX, double mouseY) {
        return listRowIndexAtInternal(mouseX, mouseY);
    }

    private int listRowIndexAtInternal(double mouseX, double mouseY) {
        if (!listView.contains(mouseX, mouseY)) {
            return -1;
        }
        int index = (int) ((mouseY - listView.y() + listScroll) / ROW_HEIGHT);
        return index >= 0 && index < listElements.size() ? index : -1;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        renderToolbar(ctx, theme, mouseX, mouseY);
        renderElementList(ctx, theme, mouseX, mouseY);
        renderProperties(ctx, theme, mouseX, mouseY);
        renderHints(ctx, theme);
    }

    private void renderToolbar(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        String title = "HUD Editor";
        String[] labels = {"Grid", "Snap", "Undo", "Redo", "Reset All"};
        int pad = 6;
        int gap = 4;
        int buttonH = 16;
        int titleW = ctx.uiTextWidth(title);
        int total = titleW + 10;
        int[] widths = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            widths[i] = ctx.uiTextWidth(labels[i]) + pad * 2;
            total += widths[i] + gap;
        }
        int x = (ctx.screenWidth() - total - pad * 2) / 2;
        int y = 6;
        toolbar = new Rect(x, y, total + pad * 2, buttonH + 8);
        UiChrome.editorPanel(ctx, theme, toolbar.x(), toolbar.y(), toolbar.w(), toolbar.h());
        int textY = y + 4 + (buttonH - ctx.uiFontHeight()) / 2;
        int cursor = x + pad;
        ctx.drawUiText(title, cursor, textY, theme.accent());
        cursor += titleW + 10;
        Rect[] rects = new Rect[labels.length];
        boolean[] active = {editor.gridShown(), editor.snapOn(), false, false, false};
        boolean[] enabled = {true, true, editor.canUndo(), editor.canRedo(), true};
        for (int i = 0; i < labels.length; i++) {
            rects[i] = new Rect(cursor, y + 4, widths[i], buttonH);
            boolean hover = enabled[i] && rects[i].contains(mouseX, mouseY);
            UiChrome.button(ctx, theme, cursor, y + 4, widths[i], buttonH, hover, active[i]);
            int color = !enabled[i] ? ColorUtil.withAlpha(theme.foregroundMuted(), 0.5f)
                    : active[i] ? 0xFFFFFFFF : theme.foreground();
            ctx.drawUiText(labels[i], cursor + pad, textY, color);
            cursor += widths[i] + gap;
        }
        btnGrid = rects[0];
        btnSnap = rects[1];
        btnUndo = rects[2];
        btnRedo = rects[3];
        btnResetAll = rects[4];
    }

    private void renderElementList(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        listElements.clear();
        listElements.addAll(editor.hud().all());
        int x = 6;
        int dockTop = editor.selected() != null
                ? bottomDockTop(ctx.screenHeight())
                : ctx.screenHeight() - HINT_RESERVE - 4;
        int h = Math.max(40, dockTop - PANEL_TOP - 4);
        listPanel = new Rect(x, PANEL_TOP, LIST_WIDTH, h);
        UiChrome.editorPanel(ctx, theme, x, PANEL_TOP, LIST_WIDTH, h);
        ctx.drawUiText("Elements", x + 8, PANEL_TOP + 6, theme.foregroundMuted());
        int viewY = PANEL_TOP + 6 + ctx.uiFontHeight() + 4;
        listView = new Rect(x + 4, viewY, LIST_WIDTH - 8, PANEL_TOP + h - viewY - 6);
        listContentHeight = listElements.size() * ROW_HEIGHT;
        int maxScroll = Math.max(0, listContentHeight - listView.h());
        listScroll = HudEditor.clampSafe(listScroll, 0, maxScroll);

        ctx.pushClip(listView.x(), listView.y(), listView.w(), listView.h());
        int rowY = listView.y() - Math.round(listScroll);
        for (HudElement element : listElements) {
            if (rowY + ROW_HEIGHT >= listView.y() && rowY < listView.y() + listView.h()) {
                boolean isSelected = element == editor.selected();
                boolean hover = listView.contains(mouseX, mouseY)
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                if (isSelected || hover) {
                    ctx.fillRoundedRect(listView.x(), rowY, listView.w(), ROW_HEIGHT - 1, PrimeDesign.RADIUS_SM,
                            ColorUtil.withAlpha(isSelected ? theme.accent() : theme.surfaceElevated(),
                                    isSelected ? 0.30f : 0.55f));
                }
                int nameColor = element.isVisible() ? theme.foreground()
                        : ColorUtil.withAlpha(theme.foregroundMuted(), 0.7f);
                ctx.drawUiText(truncate(ctx, element.name(), listView.w() - 20),
                        listView.x() + 3, rowY + (ROW_HEIGHT - ctx.uiFontHeight()) / 2, nameColor);
                int dot = element.isVisible() ? theme.success() : ColorUtil.withAlpha(theme.error(), 0.8f);
                ctx.fillRoundedRect(listView.x() + listView.w() - 9, rowY + (ROW_HEIGHT - 5) / 2, 5, 5, 2, dot);
            }
            rowY += ROW_HEIGHT;
        }
        ctx.popClip();

        if (maxScroll > 0) {
            int barH = Math.max(8, listView.h() * listView.h() / listContentHeight);
            int barY = listView.y() + Math.round((listView.h() - barH) * (listScroll / maxScroll));
            ctx.fillRoundedRect(x + LIST_WIDTH - 4, barY, 2, barH, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.6f));
        }
    }

    private void renderProperties(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        HudElement selected = editor.selected();
        if (selected == null) {
            propsPanel = Rect.EMPTY;
            btnVisibility = Rect.EMPTY;
            trackScale = Rect.EMPTY;
            trackOpacity = Rect.EMPTY;
            trackRotation = Rect.EMPTY;
            swatchRow = Rect.EMPTY;
            btnCenterX = Rect.EMPTY;
            btnCenterY = Rect.EMPTY;
            btnReset = Rect.EMPTY;
            return;
        }
        int pad = 6;
        int fontH = ctx.uiFontHeight();
        int x = LIST_WIDTH + 12;
        int w = ctx.screenWidth() - x - 6;
        int y = bottomDockTop(ctx.screenHeight());
        int h = BOTTOM_DOCK_HEIGHT;
        propsPanel = new Rect(x, y, w, h);
        UiChrome.editorPanel(ctx, theme, x, y, w, h);

        int row1Y = y + 5;
        int nameW = Math.min(ctx.uiTextWidth(selected.name()) + 4, 72);
        ctx.drawUiText(truncate(ctx, selected.name(), 68), x + pad, row1Y, theme.foreground());

        boolean visible = selected.isVisible();
        btnVisibility = new Rect(x + pad + nameW + 4, row1Y - 1, 52, 14);
        UiChrome.button(ctx, theme, btnVisibility.x(), btnVisibility.y(), btnVisibility.w(), 14,
                btnVisibility.contains(mouseX, mouseY), visible);
        String visLabel = visible ? "Show" : "Hide";
        ctx.drawUiText(visLabel, btnVisibility.x() + (btnVisibility.w() - ctx.uiTextWidth(visLabel)) / 2,
                row1Y, visible ? 0xFFFFFFFF : theme.foregroundMuted());

        int sliderX = btnVisibility.x() + btnVisibility.w() + 8;
        int sliderW = Math.max(60, (x + w - pad - sliderX - 8) / 3);
        trackScale = renderSlider(ctx, theme, sliderX, row1Y, sliderW, "Scale",
                String.format("%.1fx", selected.scale()),
                (selected.scale() - HudElement.MIN_SCALE) / (HudElement.MAX_SCALE - HudElement.MIN_SCALE));
        trackOpacity = renderSlider(ctx, theme, sliderX + sliderW + 6, row1Y, sliderW, "Opacity",
                String.format("%.0f%%", selected.opacity() * 100f),
                (selected.opacity() - HudElement.MIN_OPACITY) / (HudElement.MAX_OPACITY - HudElement.MIN_OPACITY));
        float rotation = normalizedRotation(selected);
        trackRotation = renderSlider(ctx, theme, sliderX + (sliderW + 6) * 2, row1Y, sliderW, "Rot",
                String.format("%.0fÂ°", rotation),
                (rotation + 180f) / 360f);

        int row2Y = y + h - SWATCH - 8;
        swatchRow = new Rect(x + pad, row2Y, HudEditor.TINT_PRESETS.length * (SWATCH + 2), SWATCH);
        for (int i = 0; i < HudEditor.TINT_PRESETS.length; i++) {
            int tint = HudEditor.TINT_PRESETS[i];
            int sx = x + pad + i * (SWATCH + 2);
            boolean current = selected.tintArgb() == tint;
            if (tint == 0) {
                ctx.fillRoundedBorder(sx, row2Y, SWATCH, SWATCH, PrimeDesign.RADIUS_SM, 1,
                        current ? theme.accent() : theme.border(), theme.backgroundLight());
            } else {
                ctx.fillRoundedBorder(sx, row2Y, SWATCH, SWATCH, PrimeDesign.RADIUS_SM, 1,
                        current ? theme.accent() : ColorUtil.withAlpha(theme.border(), 0.5f), tint);
            }
        }

        int btnW = 54;
        int btnY = row2Y - 1;
        btnCenterX = new Rect(x + w - pad - btnW * 3 - 8, btnY, btnW, 14);
        btnCenterY = new Rect(x + w - pad - btnW * 2 - 4, btnY, btnW, 14);
        btnReset = new Rect(x + w - pad - btnW, btnY, btnW, 14);
        drawSmallButton(ctx, theme, btnCenterX, "Center X", mouseX, mouseY, theme.foreground());
        drawSmallButton(ctx, theme, btnCenterY, "Center Y", mouseX, mouseY, theme.foreground());
        drawSmallButton(ctx, theme, btnReset, "Reset", mouseX, mouseY, theme.warning());

        String info = selected.anchor().name() + "  "
                + Math.round(selected.offsetX()) + ", " + Math.round(selected.offsetY());
        ctx.drawUiText(truncate(ctx, info, 120), swatchRow.x() + swatchRow.w() + 8, row2Y + 1,
                ColorUtil.withAlpha(theme.foregroundMuted(), 0.85f));
    }

    private void drawSmallButton(RenderContext ctx, Theme theme, Rect rect, String label,
                                 double mouseX, double mouseY, int textColor) {
        UiChrome.button(ctx, theme, rect.x(), rect.y(), rect.w(), rect.h(),
                rect.contains(mouseX, mouseY), false);
        ctx.drawUiText(label, rect.x() + (rect.w() - ctx.uiTextWidth(label)) / 2,
                rect.y() + (rect.h() - ctx.uiFontHeight()) / 2, textColor);
    }

    private Rect renderSlider(RenderContext ctx, Theme theme, int x, int y, int w,
                              String label, String value, float t) {
        ctx.drawUiText(label, x, y, theme.foregroundMuted());
        ctx.drawUiText(value, x + w - ctx.uiTextWidth(value), y, theme.foreground());
        int trackY = y + ctx.uiFontHeight() + 3;
        Rect track = new Rect(x, trackY, w, 4);
        ctx.fillRoundedRect(x, trackY, w, 4, 2, ColorUtil.withAlpha(theme.backgroundLight(), 0.9f));
        int fill = Math.round(HudEditor.clampSafe(t, 0f, 1f) * w);
        if (fill > 0) {
            ctx.fillRoundedRect(x, trackY, fill, 4, 2, theme.accent());
        }
        int knobX = x + Math.round(HudEditor.clampSafe(t, 0f, 1f) * (w - 4));
        ctx.fillRoundedRect(knobX, trackY - 2, 4, 8, 2, 0xFFFFFFFF);
        return track;
    }

    private void renderHints(RenderContext ctx, Theme theme) {
        int fontH = ctx.uiFontHeight();
        String line = HudEditorHints.LINE_1;
        int w = Math.min(ctx.screenWidth() - 24, ctx.uiTextWidth(line) + 20);
        int x = (ctx.screenWidth() - w) / 2;
        int y = ctx.screenHeight() - fontH - 6;
        hintBar = new Rect(x, y - 3, w, fontH + 8);
        ctx.fillRoundedRect(hintBar.x(), hintBar.y(), hintBar.w(), hintBar.h(),
                PrimeDesign.RADIUS_SM, ColorUtil.withAlpha(theme.surfaceElevated(), 0.55f));
        ctx.drawUiText(line, x + 10, y, ColorUtil.withAlpha(theme.foregroundMuted(), 0.9f));
    }

    private static String truncate(RenderContext ctx, String text, int maxWidth) {
        if (ctx.uiTextWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "â€¦";
        int budget = maxWidth - ctx.uiTextWidth(ellipsis);
        if (budget <= 0) {
            return ellipsis;
        }
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (ctx.uiTextWidth(text.substring(0, mid)) <= budget) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return text.substring(0, lo) + ellipsis;
    }
}
