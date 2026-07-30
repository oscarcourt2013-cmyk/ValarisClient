package dev.valerisclient.core.hud.editor;

import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.design.ValerisDesign;
import dev.valerisclient.core.gui.UiChrome;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD editor chrome: top toolbar, element list (left), properties dock (bottom)
 * and the hint bar. Geometry is computed each frame in {@link #render} and
 * reused for input hit-testing between frames.
 *
 * <p>Every dimension here is derived from the canvas size rather than fixed,
 * because the effective GUI resolution can be as small as 320x240 at high GUI
 * scale. At that size fixed chrome buries the HUD it is meant to be editing, so
 * the list collapses, the dock reflows, and {@link #toggleChrome()} hides the
 * whole thing.</p>
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
    private static final int SWATCH = 12;

    /**
     * Vertical space kept clear for the hint bar.
     *
     * <p>Derived from the bar it has to hold. It was a flat 14 while the bar is
     * {@code fontH + 8} tall, so the bar ran off the bottom of the screen and the
     * hint -- the one thing telling you how to use the editor -- was clipped.</p>
     */
    private static int hintReserve(RenderContext ctx) {
        return ctx.uiFontHeight() + 8 + 8;
    }

    /** Below this a slider is too short to aim at, so the dock stacks instead. */
    private static final int SLIDER_MIN_WIDTH = 68;
    private static final int SLIDER_GAP = 6;
    /** Cap on the name column, so it cannot starve the sliders beside it. */
    private static final int NAME_MAX_WIDTH = 70;
    /** Below this the element list costs more canvas than it is worth. */
    private static final int LIST_MIN_CANVAS_WIDTH = 340;
    /** Above this there is room to grow rows and buttons to comfortable sizes. */
    private static final int ROOMY_CANVAS_HEIGHT = 320;

    private final HudEditor editor;

    // Responsive metrics, recomputed every frame before anything is laid out.
    private int rowHeight = 13;
    private int buttonH = 14;
    private int listWidth;
    private int dockHeight = 74;
    private int hintReserve = 25;
    private boolean listShown = true;
    private boolean stackedDock;

    /** When true only the hint bar draws, so the HUD itself is unobstructed. */
    private boolean chromeHidden;

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

    // Properties dock
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

    /** Hides or restores the editor chrome so the HUD can be seen while editing. */
    void toggleChrome() {
        chromeHidden = !chromeHidden;
        if (chromeHidden) {
            activeSlider = Slider.NONE;
            clearChromeRects();
        }
    }

    boolean chromeHidden() {
        return chromeHidden;
    }

    boolean isOverUi(double mouseX, double mouseY) {
        return !chromeHidden
                && (toolbar.contains(mouseX, mouseY)
                || listRowIndexAt(mouseX, mouseY) >= 0
                || propsInteractiveHit(mouseX, mouseY));
    }

    /** Toolbar, list rows and dock controls only; empty chrome never blocks a drag. */
    boolean blocksCanvasDrag(double mouseX, double mouseY) {
        return isOverUi(mouseX, mouseY);
    }

    int bottomDockTop(int screenHeight) {
        return screenHeight - hintReserve - dockHeight - 4;
    }

    /** Empty glass chrome on the panels — clicks pass through without deselecting. */
    boolean isPanelBackdrop(double mouseX, double mouseY) {
        if (chromeHidden) {
            return false;
        }
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

    private void clearChromeRects() {
        toolbar = Rect.EMPTY;
        btnGrid = Rect.EMPTY;
        btnSnap = Rect.EMPTY;
        btnUndo = Rect.EMPTY;
        btnRedo = Rect.EMPTY;
        btnResetAll = Rect.EMPTY;
        listPanel = Rect.EMPTY;
        listView = Rect.EMPTY;
        clearPropsRects();
    }

    private void clearPropsRects() {
        propsPanel = Rect.EMPTY;
        btnVisibility = Rect.EMPTY;
        trackScale = Rect.EMPTY;
        trackOpacity = Rect.EMPTY;
        trackRotation = Rect.EMPTY;
        swatchRow = Rect.EMPTY;
        btnCenterX = Rect.EMPTY;
        btnCenterY = Rect.EMPTY;
        btnReset = Rect.EMPTY;
    }

    // ------------------------------------------------------------------
    // Responsive metrics
    // ------------------------------------------------------------------

    /**
     * Space the name column may claim. Shared by the layout and by the shape
     * decision in {@link #updateMetrics}, so the two cannot disagree.
     */
    private static int nameBudget(int panelWidth) {
        return Math.max(24, Math.min(NAME_MAX_WIDTH, panelWidth / 3)) + 4;
    }

    private static int visibilityWidth(RenderContext ctx, int pad) {
        return Math.max(40, ctx.uiTextWidth("Show") + pad * 2);
    }

    private void updateMetrics(RenderContext ctx) {
        int sw = ctx.screenWidth();
        int sh = ctx.screenHeight();
        int fontH = ctx.uiFontHeight();
        hintReserve = hintReserve(ctx);

        // Grow the click targets when there is vertical room. A cramped canvas
        // keeps the tight values rather than pushing content off the bottom.
        boolean roomy = sh >= ROOMY_CANVAS_HEIGHT;
        rowHeight = roomy ? Math.max(15, fontH + 6) : Math.max(12, fontH + 3);
        buttonH = roomy ? Math.max(17, fontH + 8) : Math.max(13, fontH + 5);

        // The list is the first thing to drop: on a narrow canvas it costs a
        // third of the width, and the canvas is the thing being edited.
        listShown = sw >= LIST_MIN_CANVAS_WIDTH;
        listWidth = listShown ? Math.max(96, Math.min(148, sw / 5)) : 0;

        // Decide the dock shape from the width actually left for sliders, so a
        // narrow canvas reflows instead of overflowing the panel edge. This must
        // reserve the name and visibility button that share the slider row --
        // measuring only the panel width picks a single row that then overflows.
        int dockX = listWidth + (listShown ? 12 : 6);
        int dockW = sw - dockX - 6;
        int pad = 6;
        int sliderStart = pad + nameBudget(dockW) + 4 + visibilityWidth(ctx, pad) + 8;
        int sliderRoom = dockW - sliderStart - pad;
        stackedDock = sliderRoom <= 0 || (sliderRoom - SLIDER_GAP * 2) / 3 < SLIDER_MIN_WIDTH;

        int sliderRowH = fontH + 3 + 8;
        dockHeight = stackedDock
                // name/visibility, three stacked sliders, then swatches + buttons.
                ? 5 + buttonH + 4 + sliderRowH * 3 + 4 + buttonH + 5
                : 5 + Math.max(buttonH, sliderRowH) + 6 + Math.max(buttonH, SWATCH) + 5;

        // Never let the dock swallow the canvas; leave at least a third visible.
        int maxDock = Math.max(40, (sh - PANEL_TOP - hintReserve) * 2 / 3);
        dockHeight = Math.min(dockHeight, maxDock);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    boolean mousePressed(double mouseX, double mouseY) {
        if (chromeHidden) {
            return false;
        }
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
        if (chromeHidden || editor.selected() == null || !propsPanel.contains(mouseX, mouseY)) {
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

    /** Widens the thin slider track into a target that can actually be hit. */
    private static boolean trackHit(Rect track, double mouseX, double mouseY) {
        return track.w() > 0
                && mouseX >= track.x() - 3 && mouseX < track.x() + track.w() + 3
                && mouseY >= track.y() - 6 && mouseY < track.y() + track.h() + 6;
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
        if (chromeHidden) {
            return false;
        }
        if (listView.contains(mouseX, mouseY)) {
            int maxScroll = Math.max(0, listContentHeight - listView.h());
            if (maxScroll > 0) {
                listScroll = HudEditor.clampSafe((float) (listScroll - scrollDelta * rowHeight * 2), 0, maxScroll);
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
        if (chromeHidden || !listView.contains(mouseX, mouseY)) {
            return -1;
        }
        int index = (int) ((mouseY - listView.y() + listScroll) / rowHeight);
        return index >= 0 && index < listElements.size() ? index : -1;
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        updateMetrics(ctx);
        if (chromeHidden) {
            clearChromeRects();
            renderHints(ctx, theme);
            return;
        }
        renderToolbar(ctx, theme, mouseX, mouseY);
        renderElementList(ctx, theme, mouseX, mouseY);
        if (editor.selected() == null) {
            renderGettingStarted(ctx, theme);
        }
        renderProperties(ctx, theme, mouseX, mouseY);
        renderHints(ctx, theme);
    }

    /**
     * Shown while nothing is selected.
     *
     * <p>With no selection the dock is empty, so the editor opened to a toolbar, a
     * list of names and no indication that picking one is the first move. This
     * spells out the three steps in the empty space where the dock will appear.</p>
     */
    private void renderGettingStarted(RenderContext ctx, Theme theme) {
        // Kept short on purpose: at 320px wide anything longer gets truncated, and
        // a half-sentence instruction is worse than none.
        String[] lines = {
                "Nothing selected",
                "1. Click a name, or click the HUD",
                "2. Drag to move · scroll to resize",
                "3. Tune it in the panel below"
        };
        int fontH = ctx.uiFontHeight();
        int pad = 10;
        int lineGap = 3;

        int textW = 0;
        for (String line : lines) {
            textW = Math.max(textW, ctx.uiTextWidth(line));
        }

        // Sit in the canvas the list does not already cover.
        int areaX = listWidth + (listShown ? 12 : 6);
        int areaW = ctx.screenWidth() - areaX - 6;
        int areaY = PANEL_TOP;
        int areaH = Math.max(0, ctx.screenHeight() - hintReserve - 4 - areaY);
        int w = Math.min(areaW, textW + pad * 2);
        int h = lines.length * fontH + (lines.length - 1) * lineGap + pad * 2;
        if (w < 60 || h > areaH) {
            // Not enough room to say it without covering everything; the hint bar
            // still carries the essentials.
            return;
        }
        int x = areaX + (areaW - w) / 2;
        int y = areaY + (areaH - h) / 2;

        UiChrome.editorPanel(ctx, theme, x, y, w, h);
        int ty = y + pad;
        for (int i = 0; i < lines.length; i++) {
            String line = truncate(ctx, lines[i], w - pad * 2);
            int color = i == 0 ? theme.foreground()
                    : ColorUtil.withAlpha(theme.foregroundMuted(), 0.95f);
            int tx = i == 0 ? x + (w - ctx.uiTextWidth(line)) / 2 : x + pad;
            ctx.drawUiText(line, tx, ty, color);
            ty += fontH + lineGap;
        }
    }

    private void renderToolbar(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        String title = "HUD Editor";
        String[] labels = {"Grid", "Snap", "Undo", "Redo", "Reset All"};
        int pad = 6;
        int gap = 4;
        int titleW = ctx.uiTextWidth(title);
        int[] widths = new int[labels.length];
        int buttonsW = 0;
        for (int i = 0; i < labels.length; i++) {
            widths[i] = ctx.uiTextWidth(labels[i]) + pad * 2;
            buttonsW += widths[i] + gap;
        }

        // Drop the title before the buttons when the bar cannot fit both, rather
        // than letting the row run off the edge of a narrow canvas.
        int available = ctx.screenWidth() - 12 - pad * 2;
        boolean showTitle = titleW + 10 + buttonsW <= available;
        int total = (showTitle ? titleW + 10 : 0) + buttonsW;

        int x = Math.max(6, (ctx.screenWidth() - total - pad * 2) / 2);
        int y = 6;
        toolbar = new Rect(x, y, Math.min(total + pad * 2, ctx.screenWidth() - 12), buttonH + 8);
        UiChrome.editorPanel(ctx, theme, toolbar.x(), toolbar.y(), toolbar.w(), toolbar.h());
        int textY = y + 4 + (buttonH - ctx.uiFontHeight()) / 2;
        int cursor = x + pad;
        if (showTitle) {
            ctx.drawUiText(title, cursor, textY, theme.accent());
            cursor += titleW + 10;
        }
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
        if (!listShown) {
            listPanel = Rect.EMPTY;
            listView = Rect.EMPTY;
            return;
        }
        int x = 6;
        int dockTop = editor.selected() != null
                ? bottomDockTop(ctx.screenHeight())
                : ctx.screenHeight() - hintReserve - 4;
        int h = Math.max(40, dockTop - PANEL_TOP - 4);
        listPanel = new Rect(x, PANEL_TOP, listWidth, h);
        UiChrome.editorPanel(ctx, theme, x, PANEL_TOP, listWidth, h);
        ctx.drawUiText("Elements", x + 8, PANEL_TOP + 6, theme.foregroundMuted());
        // Shown/total on the same line as the header, so it costs no row space.
        // Most elements ship hidden, and a wall of grey names with no count reads
        // as broken rather than as "these are switched off".
        int shownCount = 0;
        for (HudElement element : listElements) {
            if (element.isVisible()) {
                shownCount++;
            }
        }
        String count = shownCount + "/" + listElements.size();
        int countW = ctx.uiTextWidth(count);
        if (countW + 8 < listWidth - 8 - ctx.uiTextWidth("Elements") - 6) {
            ctx.drawUiText(count, x + listWidth - 8 - countW, PANEL_TOP + 6,
                    ColorUtil.withAlpha(theme.foregroundMuted(), 0.7f));
        }
        int viewY = PANEL_TOP + 6 + ctx.uiFontHeight() + 4;
        listView = new Rect(x + 4, viewY, listWidth - 8, PANEL_TOP + h - viewY - 6);
        listContentHeight = listElements.size() * rowHeight;
        int maxScroll = Math.max(0, listContentHeight - listView.h());
        listScroll = HudEditor.clampSafe(listScroll, 0, maxScroll);

        ctx.pushClip(listView.x(), listView.y(), listView.w(), listView.h());
        int rowY = listView.y() - Math.round(listScroll);
        for (HudElement element : listElements) {
            if (rowY + rowHeight >= listView.y() && rowY < listView.y() + listView.h()) {
                boolean isSelected = element == editor.selected();
                boolean hover = listView.contains(mouseX, mouseY)
                        && mouseY >= rowY && mouseY < rowY + rowHeight;
                if (isSelected || hover) {
                    ctx.fillRoundedRect(listView.x(), rowY, listView.w(), rowHeight - 1, ValerisDesign.RADIUS_SM,
                            ColorUtil.withAlpha(isSelected ? theme.accent() : theme.surfaceElevated(),
                                    isSelected ? 0.30f : 0.55f));
                }
                int nameColor = element.isVisible() ? theme.foreground()
                        : ColorUtil.withAlpha(theme.foregroundMuted(), 0.7f);
                ctx.drawUiText(truncate(ctx, element.name(), listView.w() - 20),
                        listView.x() + 3, rowY + (rowHeight - ctx.uiFontHeight()) / 2, nameColor);
                // Filled when shown, hollow when hidden. The state was carried by
                // colour alone, which is unreadable to anyone who does not already
                // know which colour means what.
                int dotX = listView.x() + listView.w() - 10;
                int dotY = rowY + (rowHeight - 6) / 2;
                if (element.isVisible()) {
                    ctx.fillRoundedRect(dotX, dotY, 6, 6, 3, theme.success());
                } else {
                    ctx.fillRoundedBorder(dotX, dotY, 6, 6, 3, 1,
                            ColorUtil.withAlpha(theme.foregroundMuted(), 0.8f), 0x00000000);
                }
            }
            rowY += rowHeight;
        }
        ctx.popClip();

        if (maxScroll > 0) {
            int barH = Math.max(8, listView.h() * listView.h() / listContentHeight);
            int barY = listView.y() + Math.round((listView.h() - barH) * (listScroll / maxScroll));
            ctx.fillRoundedRect(x + listWidth - 4, barY, 2, barH, 1,
                    ColorUtil.withAlpha(theme.accent(), 0.6f));
        }
    }

    private void renderProperties(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        HudElement selected = editor.selected();
        if (selected == null) {
            clearPropsRects();
            return;
        }
        int pad = 6;
        int fontH = ctx.uiFontHeight();
        int x = listWidth + (listShown ? 12 : 6);
        int w = ctx.screenWidth() - x - 6;
        int y = bottomDockTop(ctx.screenHeight());
        int h = dockHeight;
        propsPanel = new Rect(x, y, w, h);
        UiChrome.editorPanel(ctx, theme, x, y, w, h);

        int rowY = y + 5;

        // Row 1: element name and the visibility toggle.
        int nameSlot = nameBudget(w);
        String name = truncate(ctx, selected.name(), nameSlot - 4);
        ctx.drawUiText(name, x + pad, rowY + (buttonH - fontH) / 2, theme.foreground());
        // Reserve the full slot rather than the measured width, so the slider row
        // starts where updateMetrics assumed when it chose the dock shape.
        int nameW = nameSlot;

        boolean visible = selected.isVisible();
        int visW = visibilityWidth(ctx, pad);
        btnVisibility = new Rect(x + pad + nameW + 4, rowY, visW, buttonH);
        UiChrome.button(ctx, theme, btnVisibility.x(), btnVisibility.y(), btnVisibility.w(), buttonH,
                btnVisibility.contains(mouseX, mouseY), visible);
        String visLabel = visible ? "Show" : "Hide";
        ctx.drawUiText(visLabel, btnVisibility.x() + (btnVisibility.w() - ctx.uiTextWidth(visLabel)) / 2,
                btnVisibility.y() + (buttonH - fontH) / 2, visible ? 0xFFFFFFFF : theme.foregroundMuted());

        int sliderRowH = fontH + 3 + 8;
        if (stackedDock) {
            // Narrow canvas: give each slider the full panel width on its own row
            // so none of them shrinks below a usable target.
            int sx = x + pad;
            int sw = w - pad * 2;
            int sy = rowY + buttonH + 4;
            trackScale = renderSlider(ctx, theme, sx, sy, sw, "Scale",
                    String.format("%.1fx", selected.scale()),
                    (selected.scale() - HudElement.MIN_SCALE) / (HudElement.MAX_SCALE - HudElement.MIN_SCALE));
            trackOpacity = renderSlider(ctx, theme, sx, sy + sliderRowH, sw, "Opacity",
                    String.format("%.0f%%", selected.opacity() * 100f),
                    (selected.opacity() - HudElement.MIN_OPACITY) / (HudElement.MAX_OPACITY - HudElement.MIN_OPACITY));
            float rotation = normalizedRotation(selected);
            trackRotation = renderSlider(ctx, theme, sx, sy + sliderRowH * 2, sw, "Rot",
                    String.format("%.0f°", rotation),
                    (rotation + 180f) / 360f);
        } else {
            // Wide canvas: the three sliders share the space left of the panel
            // edge. Derived from what is actually free, so it cannot overflow.
            int sliderX = btnVisibility.x() + btnVisibility.w() + 8;
            int free = x + w - pad - sliderX;
            int gap = SLIDER_GAP;
            // No lower clamp here on purpose. updateMetrics already switched to
            // the stacked dock if this would come out too narrow, and flooring it
            // is exactly what pushed the row off the panel edge before.
            int sliderW = Math.max(1, (free - gap * 2) / 3);
            trackScale = renderSlider(ctx, theme, sliderX, rowY, sliderW, "Scale",
                    String.format("%.1fx", selected.scale()),
                    (selected.scale() - HudElement.MIN_SCALE) / (HudElement.MAX_SCALE - HudElement.MIN_SCALE));
            trackOpacity = renderSlider(ctx, theme, sliderX + sliderW + gap, rowY, sliderW, "Opacity",
                    String.format("%.0f%%", selected.opacity() * 100f),
                    (selected.opacity() - HudElement.MIN_OPACITY) / (HudElement.MAX_OPACITY - HudElement.MIN_OPACITY));
            float rotation = normalizedRotation(selected);
            trackRotation = renderSlider(ctx, theme, sliderX + (sliderW + gap) * 2, rowY, sliderW, "Rot",
                    String.format("%.0f°", rotation),
                    (rotation + 180f) / 360f);
        }

        // Bottom row: tint swatches on the left, actions on the right.
        int actionY = y + h - Math.max(buttonH, SWATCH) - 5;
        int swatchY = actionY + (Math.max(buttonH, SWATCH) - SWATCH) / 2;
        swatchRow = new Rect(x + pad, swatchY, HudEditor.TINT_PRESETS.length * (SWATCH + 3), SWATCH);
        for (int i = 0; i < HudEditor.TINT_PRESETS.length; i++) {
            int tint = HudEditor.TINT_PRESETS[i];
            int sx = x + pad + i * (SWATCH + 3);
            boolean current = selected.tintArgb() == tint;
            if (tint == 0) {
                ctx.fillRoundedBorder(sx, swatchY, SWATCH, SWATCH, ValerisDesign.RADIUS_SM, 1,
                        current ? theme.accent() : theme.border(), theme.backgroundLight());
            } else {
                ctx.fillRoundedBorder(sx, swatchY, SWATCH, SWATCH, ValerisDesign.RADIUS_SM, 1,
                        current ? theme.accent() : ColorUtil.withAlpha(theme.border(), 0.5f), tint);
            }
        }

        // Label the buttons to fit: "Center X" collapses to "X" when space is tight.
        int actionsRight = x + w - pad;
        int actionsLeft = swatchRow.x() + swatchRow.w() + 8;
        int gap = 4;
        boolean longLabels = (actionsRight - actionsLeft - gap * 2) / 3 >= ctx.uiTextWidth("Center X") + 8;
        String cxLabel = longLabels ? "Center X" : "X";
        String cyLabel = longLabels ? "Center Y" : "Y";
        String rsLabel = "Reset";
        int cxW = ctx.uiTextWidth(cxLabel) + pad * 2;
        int cyW = ctx.uiTextWidth(cyLabel) + pad * 2;
        int rsW = ctx.uiTextWidth(rsLabel) + pad * 2;
        btnReset = new Rect(actionsRight - rsW, actionY, rsW, buttonH);
        btnCenterY = new Rect(btnReset.x() - gap - cyW, actionY, cyW, buttonH);
        btnCenterX = new Rect(btnCenterY.x() - gap - cxW, actionY, cxW, buttonH);
        drawSmallButton(ctx, theme, btnCenterX, cxLabel, mouseX, mouseY, theme.foreground());
        drawSmallButton(ctx, theme, btnCenterY, cyLabel, mouseX, mouseY, theme.foreground());
        drawSmallButton(ctx, theme, btnReset, rsLabel, mouseX, mouseY, theme.warning());

        // Anchor/offset readout, only when there is genuinely room between the
        // swatches and the buttons -- otherwise it overlapped both.
        int infoLeft = swatchRow.x() + swatchRow.w() + 8;
        int infoRoom = btnCenterX.x() - gap - infoLeft;
        if (infoRoom > 24) {
            String info = selected.anchor().name() + "  "
                    + Math.round(selected.offsetX()) + ", " + Math.round(selected.offsetY());
            ctx.drawUiText(truncate(ctx, info, infoRoom), infoLeft,
                    actionY + (buttonH - fontH) / 2,
                    ColorUtil.withAlpha(theme.foregroundMuted(), 0.85f));
        }
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
        int valueW = ctx.uiTextWidth(value);
        ctx.drawUiText(truncate(ctx, label, Math.max(8, w - valueW - 4)), x, y, theme.foregroundMuted());
        ctx.drawUiText(value, x + w - valueW, y, theme.foreground());
        int trackY = y + ctx.uiFontHeight() + 3;
        Rect track = new Rect(x, trackY, w, 4);
        ctx.fillRoundedRect(x, trackY, w, 4, 2, ColorUtil.withAlpha(theme.backgroundLight(), 0.9f));
        int fill = Math.round(HudEditor.clampSafe(t, 0f, 1f) * w);
        if (fill > 0) {
            ctx.fillRoundedRect(x, trackY, fill, 4, 2, theme.accent());
        }
        // Wider knob than the old 4px: it is the thing being dragged.
        int knobW = 6;
        int knobX = x + Math.round(HudEditor.clampSafe(t, 0f, 1f) * (w - knobW));
        ctx.fillRoundedRect(knobX, trackY - 3, knobW, 10, 3, 0xFFFFFFFF);
        return track;
    }

    private void renderHints(RenderContext ctx, Theme theme) {
        int fontH = ctx.uiFontHeight();
        String line = chromeHidden ? HudEditorHints.HIDDEN : HudEditorHints.LINE_1;
        // Shorten to whatever the canvas can hold instead of running off the edge.
        int maxW = ctx.screenWidth() - 24;
        String shown = truncate(ctx, line, maxW - 20);
        int w = Math.min(maxW, ctx.uiTextWidth(shown) + 20);
        int x = (ctx.screenWidth() - w) / 2;
        // Anchor the bar off the bottom edge rather than off the text baseline, so
        // the whole bar sits inside the screen instead of hanging off it.
        int barH = fontH + 8;
        int barY = ctx.screenHeight() - barH - 6;
        int y = barY + 4;
        hintBar = new Rect(x, barY, w, barH);
        ctx.fillRoundedRect(hintBar.x(), hintBar.y(), hintBar.w(), hintBar.h(),
                ValerisDesign.RADIUS_SM, ColorUtil.withAlpha(theme.surfaceElevated(), 0.55f));
        ctx.drawUiText(shown, x + 10, y, ColorUtil.withAlpha(theme.foregroundMuted(), 0.9f));
    }

    private static String truncate(RenderContext ctx, String text, int maxWidth) {
        if (ctx.uiTextWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
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
