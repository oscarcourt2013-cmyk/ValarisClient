package dev.stellarclient.core.hud.editor;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;
import dev.stellarclient.core.util.ColorUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * HUD editor: selection, drag with smart snapping, keyboard nudging, undo/redo,
 * plus the toolbar / element list / properties panels of {@link HudEditorUi}.
 */
public final class HudEditor {

    static final int[] TINT_PRESETS = {
            0,
            0xFFFFFFFF,
            0xFFFF5555,
            0xFF55FF55,
            0xFF5555FF,
            0xFFFFFF55
    };

    private static final int UNDO_LIMIT = 50;
    private static final long SNAPSHOT_COALESCE_MS = 800;

    // GLFW key codes
    private static final int KEY_G = 71;
    private static final int KEY_R = 82;
    private static final int KEY_S = 83;
    private static final int KEY_V = 86;
    private static final int KEY_Y = 89;
    private static final int KEY_Z = 90;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_LEFT = 263;
    private static final int KEY_DOWN = 264;
    private static final int KEY_UP = 265;

    private final HudManager hud;
    private final ThemeManager themes;
    private final HudEditorUi ui;

    private boolean dragArmed;
    private double lastPressX;
    private double lastPressY;
    private HudElement selected;
    private HudElement dragging;
    private float grabOffsetX;
    private float grabOffsetY;
    private boolean didDrag;
    private boolean snapEnabled = true;
    private boolean showGrid = false;
    private int tintPresetIndex;

    /** Screen size seen by the last render/drag â€” keyboard nudges need it between frames. */
    private int screenWidth = 854;
    private int screenHeight = 480;

    /** Active snap guide lines during a drag; negative = none. */
    private float guideX = -1;
    private float guideY = -1;

    private final ArrayDeque<LayoutSnapshot> undoStack = new ArrayDeque<>();
    private final ArrayDeque<LayoutSnapshot> redoStack = new ArrayDeque<>();
    private LayoutSnapshot pendingSnapshot;
    private long lastCoalescedSnapshotMs;

    private Runnable autosaveHandler = () -> {};
    private boolean layoutDirty;
    private long layoutDirtyAtMs;
    private static final long AUTOSAVE_DEBOUNCE_MS = 750;

    public HudEditor(HudManager hud, ThemeManager themes) {
        this.hud = hud;
        this.themes = themes;
        this.ui = new HudEditorUi(this);
    }

    /** Invoked after debounced layout changes (profile save). */
    public void setAutosaveHandler(Runnable handler) {
        this.autosaveHandler = handler != null ? handler : () -> {};
    }

    /**
     * Flushes a pending autosave when the debounce window elapsed.
     * Call once per frame from the HUD editor screen.
     */
    public void tickAutosave() {
        if (!layoutDirty) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - layoutDirtyAtMs < AUTOSAVE_DEBOUNCE_MS) {
            return;
        }
        layoutDirty = false;
        autosaveHandler.run();
    }

    /** Forces any pending debounced save immediately (e.g. on screen close). */
    public void flushAutosave() {
        if (!layoutDirty) {
            return;
        }
        layoutDirty = false;
        autosaveHandler.run();
    }

    private void markLayoutDirty() {
        layoutDirty = true;
        layoutDirtyAtMs = System.currentTimeMillis();
    }

    public HudElement selected() {
        return selected;
    }

    HudManager hud() {
        return hud;
    }

    int screenWidth() {
        return screenWidth;
    }

    int screenHeight() {
        return screenHeight;
    }

    boolean gridShown() {
        return showGrid;
    }

    boolean snapOn() {
        return snapEnabled;
    }

    void toggleGrid() {
        showGrid = !showGrid;
    }

    void toggleSnap() {
        snapEnabled = !snapEnabled;
    }

    // ------------------------------------------------------------------
    // Mouse input
    // ------------------------------------------------------------------

    public boolean mousePressed(double mouseX, double mouseY) {
        lastPressX = mouseX;
        lastPressY = mouseY;
        dragArmed = false;
        if (ui.mousePressed(mouseX, mouseY)) {
            if (ui.listRowIndexAt(mouseX, mouseY) >= 0) {
                dragArmed = true;
            }
            return true;
        }
        HudElement hit = hud.elementAt(mouseX, mouseY, true);
        if (hit != null) {
            this.selected = hit;
            this.dragging = hit;
            this.didDrag = false;
            this.grabOffsetX = (float) (mouseX - hit.lastX());
            this.grabOffsetY = (float) (mouseY - hit.lastY());
            dragArmed = true;
            beginGesture();
            return true;
        }
        if (selected != null && !ui.blocksCanvasDrag(mouseX, mouseY) && !ui.isPanelBackdrop(mouseX, mouseY)) {
            dragArmed = true;
            this.dragging = null;
            this.didDrag = false;
            return true;
        }
        if (!ui.isPanelBackdrop(mouseX, mouseY)) {
            this.selected = null;
        }
        this.dragging = null;
        return false;
    }

    public void mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        if (ui.mouseDragged(mouseX, mouseY)) {
            return;
        }
        HudElement element = dragging;
        if (element == null && dragArmed && selected != null && !ui.blocksCanvasDrag(mouseX, mouseY)) {
            element = selected;
            dragging = selected;
            grabOffsetX = (float) (lastPressX - selected.lastX());
            grabOffsetY = (float) (lastPressY - selected.lastY());
            beginGesture();
        }
        if (element == null) {
            return;
        }
        markMutated();
        didDrag = true;
        float width = element.lastWidth();
        float height = element.lastHeight();
        float x = clampSafe((float) mouseX - grabOffsetX, 0, screenWidth - width);
        float y = clampSafe((float) mouseY - grabOffsetY, 0, screenHeight - height);
        guideX = -1;
        guideY = -1;
        if (snapEnabled) {
            x = clampSafe(applySnapX(element, x, width, screenWidth), 0, screenWidth - width);
            y = clampSafe(applySnapY(element, y, height, screenHeight), 0, screenHeight - height);
        }
        moveTo(element, x, y);
    }

    public void mouseReleased() {
        ui.mouseReleased();
        this.dragging = null;
        this.dragArmed = false;
        this.pendingSnapshot = null;
        this.guideX = -1;
        this.guideY = -1;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta, boolean shiftDown, boolean ctrlDown) {
        if (ui.mouseScrolled(mouseX, mouseY, scrollDelta)) {
            return true;
        }
        HudElement target = selected != null && selected.containsPoint(mouseX, mouseY)
                ? selected
                : hud.elementAt(mouseX, mouseY, true);
        if (target == null) {
            return false;
        }
        snapshotCoalesced();
        float step = (float) Math.signum(scrollDelta);
        if (shiftDown) {
            target.setRotation(target.rotation() + step * 5f);
            return true;
        }
        if (ctrlDown) {
            target.setOpacity(target.opacity() + step * 0.05f);
            return true;
        }
        target.setScale(target.scale() + step * 0.1f);
        return true;
    }

    // ------------------------------------------------------------------
    // Keyboard input
    // ------------------------------------------------------------------

    public boolean keyPressed(int glfwKey) {
        return keyPressed(glfwKey, false, false);
    }

    /** G grid Â· S snap Â· V visibility Â· R tint Â· arrows nudge (Shift=10px) Â· Ctrl+Z/Y undo/redo. */
    public boolean keyPressed(int glfwKey, boolean shiftDown, boolean ctrlDown) {
        if (ctrlDown && glfwKey == KEY_Z) {
            if (shiftDown) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (ctrlDown && glfwKey == KEY_Y) {
            redo();
            return true;
        }
        if (glfwKey == KEY_G) {
            showGrid = !showGrid;
            return true;
        }
        if (glfwKey == KEY_S) {
            snapEnabled = !snapEnabled;
            return true;
        }
        if (selected == null) {
            return false;
        }
        float nudge = shiftDown ? 10f : 1f;
        switch (glfwKey) {
            case KEY_LEFT -> {
                nudgeSelected(-nudge, 0);
                return true;
            }
            case KEY_RIGHT -> {
                nudgeSelected(nudge, 0);
                return true;
            }
            case KEY_UP -> {
                nudgeSelected(0, -nudge);
                return true;
            }
            case KEY_DOWN -> {
                nudgeSelected(0, nudge);
                return true;
            }
            case KEY_V -> {
                snapshotNow();
                selected.setVisible(!selected.isVisible());
                return true;
            }
            case KEY_R -> {
                snapshotNow();
                tintPresetIndex = (tintPresetIndex + 1) % TINT_PRESETS.length;
                selected.setTintArgb(TINT_PRESETS[tintPresetIndex]);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------
    // Editing operations (shared by keys and UI panels)
    // ------------------------------------------------------------------

    void select(HudElement element) {
        this.selected = element;
        if (element != null && isLost(element)) {
            snapshotNow();
            moveTo(element,
                    (screenWidth - element.lastWidth()) / 2f,
                    (screenHeight - element.lastHeight()) / 2f);
        }
    }

    /** True when the element cannot be grabbed on the canvas (zero-size or fully off-screen). */
    private boolean isLost(HudElement element) {
        return element.lastWidth() <= 0
                || element.lastHeight() <= 0
                || element.lastX() + element.lastWidth() <= 0
                || element.lastX() >= screenWidth
                || element.lastY() + element.lastHeight() <= 0
                || element.lastY() >= screenHeight;
    }

    private void nudgeSelected(float dx, float dy) {
        HudElement element = selected;
        snapshotCoalesced();
        // Work from anchor + offset, not lastX/lastY: those only refresh on the next
        // layout pass, and key-repeat can fire several nudges within one frame.
        float width = element.lastWidth();
        float height = element.lastHeight();
        float baseX = element.anchor().baseX(screenWidth, width);
        float baseY = element.anchor().baseY(screenHeight, height);
        float x = clampSafe(baseX + element.offsetX() + dx, 0, screenWidth - width);
        float y = clampSafe(baseY + element.offsetY() + dy, 0, screenHeight - height);
        element.setLayout(element.anchor(), x - baseX, y - baseY);
    }

    void centerSelectedX() {
        if (selected == null) {
            return;
        }
        snapshotNow();
        moveTo(selected, (screenWidth - selected.lastWidth()) / 2f, selected.lastY());
    }

    void centerSelectedY() {
        if (selected == null) {
            return;
        }
        snapshotNow();
        moveTo(selected, selected.lastX(), (screenHeight - selected.lastHeight()) / 2f);
    }

    void resetSelected() {
        if (selected == null) {
            return;
        }
        snapshotNow();
        selected.resetToDefaults();
    }

    void resetAll() {
        snapshotNow();
        for (HudElement element : hud.all()) {
            element.resetToDefaults();
        }
    }

    void toggleVisibility(HudElement element) {
        snapshotNow();
        element.setVisible(!element.isVisible());
    }

    void setSelectedTint(int argb) {
        if (selected == null) {
            return;
        }
        snapshotNow();
        selected.setTintArgb(argb);
    }

    /** Re-anchors {@code element} so its top-left lands on ({@code x}, {@code y}). */
    void moveTo(HudElement element, float x, float y) {
        float width = element.lastWidth();
        float height = element.lastHeight();
        HudAnchor anchor = HudAnchor.closest(
                (x + width / 2f) / Math.max(1, screenWidth),
                (y + height / 2f) / Math.max(1, screenHeight));
        element.setLayout(anchor,
                x - anchor.baseX(screenWidth, width),
                y - anchor.baseY(screenHeight, height));
    }

    /**
     * Clamp that survives elements larger than the screen ({@code max < min})
     * instead of throwing like {@link Math#clamp}.
     */
    static float clampSafe(float value, float min, float max) {
        if (max < min) {
            return min;
        }
        return Math.clamp(value, min, max);
    }

    // ------------------------------------------------------------------
    // Snapping
    // ------------------------------------------------------------------

    /** Best snap candidate for one axis: distance, snapped position, guide line. */
    private static final class SnapResult {
        float delta = Float.MAX_VALUE;
        float value;
        float guide = -1;

        void consider(float snappedPos, float guideLine, float current) {
            float d = Math.abs(snappedPos - current);
            if (d < delta) {
                delta = d;
                value = snappedPos;
                guide = guideLine;
            }
        }
    }

    private float applySnapX(HudElement dragged, float x, float width, int screenWidth) {
        SnapResult best = new SnapResult();
        for (HudElement other : hud.all()) {
            if (other == dragged || !other.isVisible() || other.lastWidth() <= 0) {
                continue;
            }
            considerTargetX(best, x, width, other.lastX());
            considerTargetX(best, x, width, other.lastX() + other.lastWidth() / 2f);
            considerTargetX(best, x, width, other.lastX() + other.lastWidth());
        }
        considerTargetX(best, x, width, 0);
        considerTargetX(best, x, width, screenWidth / 2f);
        considerTargetX(best, x, width, screenWidth);
        if (best.delta <= PrimeDesign.SNAP_THRESHOLD) {
            guideX = best.guide;
            return best.value;
        }
        return Math.round(x / (float) PrimeDesign.GRID_SIZE) * PrimeDesign.GRID_SIZE;
    }

    /** Aligns the dragged box's left edge, center or right edge onto {@code target}. */
    private static void considerTargetX(SnapResult best, float x, float width, float target) {
        best.consider(target, target, x);
        best.consider(target - width / 2f, target, x);
        best.consider(target - width, target, x);
    }

    private float applySnapY(HudElement dragged, float y, float height, int screenHeight) {
        SnapResult best = new SnapResult();
        for (HudElement other : hud.all()) {
            if (other == dragged || !other.isVisible() || other.lastHeight() <= 0) {
                continue;
            }
            considerTargetY(best, y, height, other.lastY());
            considerTargetY(best, y, height, other.lastY() + other.lastHeight() / 2f);
            considerTargetY(best, y, height, other.lastY() + other.lastHeight());
        }
        considerTargetY(best, y, height, 0);
        considerTargetY(best, y, height, screenHeight / 2f);
        considerTargetY(best, y, height, screenHeight);
        if (best.delta <= PrimeDesign.SNAP_THRESHOLD) {
            guideY = best.guide;
            return best.value;
        }
        return Math.round(y / (float) PrimeDesign.GRID_SIZE) * PrimeDesign.GRID_SIZE;
    }

    private static void considerTargetY(SnapResult best, float y, float height, float target) {
        best.consider(target, target, y);
        best.consider(target - height / 2f, target, y);
        best.consider(target - height, target, y);
    }

    // ------------------------------------------------------------------
    // Undo / redo
    // ------------------------------------------------------------------

    /** Arms a lazy snapshot; committed by {@link #markMutated()} on the first real change. */
    void beginGesture() {
        pendingSnapshot = LayoutSnapshot.capture(hud);
    }

    void markMutated() {
        if (pendingSnapshot != null) {
            commit(pendingSnapshot);
            pendingSnapshot = null;
        }
        markLayoutDirty();
    }

    /** Immediate snapshot for one-shot operations (buttons, key toggles). */
    void snapshotNow() {
        commit(LayoutSnapshot.capture(hud));
        markLayoutDirty();
    }

    /** Snapshot for rapid repeated inputs (scroll, held arrow keys): one entry per burst. */
    private void snapshotCoalesced() {
        long now = System.currentTimeMillis();
        if (now - lastCoalescedSnapshotMs > SNAPSHOT_COALESCE_MS) {
            snapshotNow();
        } else {
            markLayoutDirty();
        }
        lastCoalescedSnapshotMs = now;
    }

    private void commit(LayoutSnapshot snapshot) {
        undoStack.addLast(snapshot);
        while (undoStack.size() > UNDO_LIMIT) {
            undoStack.removeFirst();
        }
        redoStack.clear();
    }

    boolean canUndo() {
        return !undoStack.isEmpty();
    }

    boolean canRedo() {
        return !redoStack.isEmpty();
    }

    boolean undo() {
        LayoutSnapshot snapshot = undoStack.pollLast();
        if (snapshot == null) {
            return false;
        }
        redoStack.addLast(LayoutSnapshot.capture(hud));
        snapshot.restore();
        markLayoutDirty();
        return true;
    }

    boolean redo() {
        LayoutSnapshot snapshot = redoStack.pollLast();
        if (snapshot == null) {
            return false;
        }
        undoStack.addLast(LayoutSnapshot.capture(hud));
        snapshot.restore();
        markLayoutDirty();
        return true;
    }

    private record ElementState(HudElement element, HudAnchor anchor, float offsetX, float offsetY,
                                float scale, float rotation, float opacity, int tint, boolean visible) {

        static ElementState capture(HudElement element) {
            return new ElementState(element, element.anchor(), element.offsetX(), element.offsetY(),
                    element.scale(), element.rotation(), element.opacity(), element.tintArgb(), element.isVisible());
        }

        void restore() {
            element.setLayout(anchor, offsetX, offsetY);
            element.setScale(scale);
            element.setRotation(rotation);
            element.setOpacity(opacity);
            element.setTintArgb(tint);
            element.setVisible(visible);
        }
    }

    private record LayoutSnapshot(List<ElementState> states) {

        static LayoutSnapshot capture(HudManager hud) {
            List<ElementState> states = new ArrayList<>();
            for (HudElement element : hud.all()) {
                states.add(ElementState.capture(element));
            }
            return new LayoutSnapshot(states);
        }

        void restore() {
            for (ElementState state : states) {
                state.restore();
            }
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    public void renderOverlay(RenderContext ctx, double mouseX, double mouseY) {
        this.screenWidth = ctx.screenWidth();
        this.screenHeight = ctx.screenHeight();
        Theme theme = themes.active();
        if (showGrid) {
            drawGrid(ctx, theme);
        }
        boolean overUi = ui.isOverUi(mouseX, mouseY);
        HudElement listHover = ui.hoveredListElement(mouseX, mouseY);
        for (HudElement element : hud.all()) {
            boolean isSelected = element == selected;
            boolean isHovered = element == listHover
                    || (!overUi && element.containsPoint(mouseX, mouseY));
            if (!isSelected && !isHovered) {
                if (!element.isVisible()) {
                    drawBorder(ctx, element, ColorUtil.withAlpha(theme.foregroundMuted(), 0.25f));
                }
                continue;
            }
            if (isSelected) {
                drawSelection(ctx, theme, element);
            } else {
                drawBorder(ctx, element, ColorUtil.withAlpha(
                        element.isVisible() ? theme.foreground() : theme.foregroundMuted(), 0.6f));
            }
        }
        drawSnapGuides(ctx, theme);
        if (dragging != null && didDrag) {
            drawDragBadge(ctx, theme, dragging);
        }
        ui.render(ctx, theme, mouseX, mouseY);
    }

    private void drawSelection(RenderContext ctx, Theme theme, HudElement element) {
        int x = Math.round(element.lastX()) - 2;
        int y = Math.round(element.lastY()) - 2;
        int w = Math.round(element.lastWidth()) + 4;
        int h = Math.round(element.lastHeight()) + 4;
        int accent = element.isVisible() ? theme.accent() : ColorUtil.withAlpha(theme.accent(), 0.5f);
        ctx.fillRect(x, y, w, 1, accent);
        ctx.fillRect(x, y + h - 1, w, 1, accent);
        ctx.fillRect(x, y + 1, 1, h - 2, accent);
        ctx.fillRect(x + w - 1, y + 1, 1, h - 2, accent);
        int handle = 3;
        ctx.fillRect(x - 1, y - 1, handle, handle, accent);
        ctx.fillRect(x + w - 2, y - 1, handle, handle, accent);
        ctx.fillRect(x - 1, y + h - 2, handle, handle, accent);
        ctx.fillRect(x + w - 2, y + h - 2, handle, handle, accent);
        String label = element.isVisible() ? element.name() : element.name() + " (hidden)";
        int labelW = ctx.uiTextWidth(label) + 8;
        int labelH = ctx.uiFontHeight() + 4;
        int labelX = (int) clampSafe(x + (w - labelW) / 2f, 0, screenWidth - labelW);
        int labelY = y - labelH - 2 < 0 ? y + h + 3 : y - labelH - 2;
        ctx.fillRoundedRect(labelX, labelY, labelW, labelH, PrimeDesign.RADIUS_SM,
                ColorUtil.withAlpha(theme.background(), 0.9f));
        ctx.drawUiText(label, labelX + 4, labelY + 2, theme.foreground());
    }

    private static void drawBorder(RenderContext ctx, HudElement element, int argb) {
        int x = Math.round(element.lastX()) - 1;
        int y = Math.round(element.lastY()) - 1;
        int w = Math.round(element.lastWidth()) + 2;
        int h = Math.round(element.lastHeight()) + 2;
        ctx.fillRect(x, y, w, 1, argb);
        ctx.fillRect(x, y + h - 1, w, 1, argb);
        ctx.fillRect(x, y + 1, 1, h - 2, argb);
        ctx.fillRect(x + w - 1, y + 1, 1, h - 2, argb);
    }

    private void drawSnapGuides(RenderContext ctx, Theme theme) {
        if (dragging == null) {
            return;
        }
        int color = ColorUtil.withAlpha(theme.accent(), 0.66f);
        if (guideX >= 0) {
            ctx.fillRect(Math.round(guideX), 0, 1, screenHeight, color);
        }
        if (guideY >= 0) {
            ctx.fillRect(0, Math.round(guideY), screenWidth, 1, color);
        }
    }

    private void drawDragBadge(RenderContext ctx, Theme theme, HudElement element) {
        String text = Math.round(element.lastX()) + ", " + Math.round(element.lastY());
        int w = ctx.uiTextWidth(text) + 8;
        int h = ctx.uiFontHeight() + 4;
        int x = (int) clampSafe(Math.round(element.lastX()), 0, screenWidth - w);
        int y = Math.round(element.lastY() + element.lastHeight()) + 4;
        if (y + h > screenHeight) {
            y = Math.round(element.lastY()) - h - 4;
        }
        ctx.fillRoundedRect(x, y, w, h, PrimeDesign.RADIUS_SM, ColorUtil.withAlpha(theme.background(), 0.9f));
        ctx.drawUiText(text, x + 4, y + 2, theme.accent());
    }

    private void drawGrid(RenderContext ctx, Theme theme) {
        int grid = PrimeDesign.GRID_SIZE * 2;
        int color = ColorUtil.withAlpha(theme.border(), 0.18f);
        for (int x = grid; x < screenWidth; x += grid) {
            ctx.fillRect(x, 0, 1, screenHeight, color);
        }
        for (int y = grid; y < screenHeight; y += grid) {
            ctx.fillRect(0, y, screenWidth, 1, color);
        }
        int center = ColorUtil.withAlpha(theme.accent(), dragging != null ? 0.35f : 0.15f);
        ctx.fillRect(screenWidth / 2, 0, 1, screenHeight, center);
        ctx.fillRect(0, screenHeight / 2, screenWidth, 1, center);
    }
}
