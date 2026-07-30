package dev.valarisclient.core.gui.clickgui;

import dev.valarisclient.core.adapter.RenderContext;
import dev.valarisclient.core.design.ValarisDesign;
import dev.valarisclient.core.gui.FavoritesManager;
import dev.valarisclient.core.gui.GuiLayout;
import dev.valarisclient.core.gui.UiChrome;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.module.ModuleManager;
import dev.valarisclient.core.theme.Theme;
import dev.valarisclient.core.util.ColorUtil;
import dev.valarisclient.core.util.Easing;

import java.util.List;

/**
 * Card-based module browser: pill category filters + a grid of module cards.
 *
 * <p>Card size is supplied by {@link ClickGuiBrowseLayout} rather than fixed,
 * so the grid reflows for any effective GUI resolution.</p>
 */
public final class ModuleCardBrowser {

    public static final int TAB_H = 16;

    private static final int CARD_GAP = ClickGuiBrowseLayout.CARD_GAP;
    private static final int SIDE_PAD = 6;
    private static final int PILL_H = 12;
    private static final int OPTIONS_H = 12;
    private static final int TAB_PAD = 16;
    private static final float ICON_SCALE_MAX = 2.0f;

    private final ModuleManager modules;
    private final FavoritesManager favorites;

    private static final int SCROLLBAR_W = 4;
    private static final int SCROLLBAR_MIN_THUMB = 16;

    private ModuleCategory activeCategory = ModuleCategory.PVP;
    private String searchQuery = "";
    private Module selected;
    private float scrollY;
    private float targetScrollY;

    /** Drag state for the scrollbar thumb and for click-drag panning of the grid. */
    private boolean draggingThumb;
    private boolean draggingGrid;
    private double dragAnchorY;
    private float dragAnchorScroll;
    private boolean dragMoved;

    public ModuleCardBrowser(ModuleManager modules, FavoritesManager favorites) {
        this.modules = modules;
        this.favorites = favorites;
    }

    public Module selected() {
        return selected;
    }

    /** Global search text - when non-blank, overrides the category filter. */
    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query;
    }

    public boolean isSearching() {
        return !searchQuery.isBlank();
    }

    public void tick(float deltaSeconds) {
        scrollY = Easing.lerp(scrollY, targetScrollY, deltaSeconds * 14f);
    }

    public void render(RenderContext ctx, Theme theme, ClickGuiBrowseLayout layout,
                       double mouseX, double mouseY) {
        int x = layout.gridX();
        int y = layout.gridY();
        int width = layout.gridW();
        int height = layout.gridH();
        if (width <= 0 || height <= 0) {
            return;
        }

        ctx.pushClip(x, y, width, TAB_H);
        renderCategoryPills(ctx, theme, x, y, width, mouseX, mouseY);
        ctx.popClip();

        int cardW = layout.cardW();
        int cardH = layout.cardH();
        int cols = Math.max(1, layout.columns());
        int contentY = y + TAB_H + CARD_GAP;
        int contentH = Math.max(0, height - TAB_H - CARD_GAP);

        List<Module> list = filteredModules();
        int rowSpan = cardH + CARD_GAP;
        int rows = (list.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, rows * rowSpan - contentH);
        if (targetScrollY > maxScroll) {
            targetScrollY = maxScroll;
        }
        if (scrollY > maxScroll) {
            scrollY = maxScroll;
        }

        ctx.pushClip(x, contentY, width, contentH);
        int startRow = Math.max(0, (int) (scrollY / rowSpan));
        int endRow = Math.min(rows, startRow + contentH / rowSpan + 2);
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                if (i >= list.size()) {
                    break;
                }
                int cx = x + col * (cardW + CARD_GAP);
                int cy = contentY + row * rowSpan - Math.round(scrollY);
                if (cy + cardH < contentY || cy > contentY + contentH) {
                    continue;
                }
                renderCard(ctx, theme, list.get(i), cx, cy, cardW, cardH, mouseX, mouseY);
            }
        }
        ctx.popClip();

        renderScrollbar(ctx, theme, x, contentY, width, contentH, maxScroll, mouseX, mouseY);
    }

    /**
     * Visible scrollbar with a draggable thumb. Wheel-less input (touchpads without
     * two-finger scroll, or click-drag) can still reach off-screen rows.
     */
    private void renderScrollbar(RenderContext ctx, Theme theme, int x, int contentY, int width,
                                 int contentH, int maxScroll, double mouseX, double mouseY) {
        if (maxScroll <= 0 || contentH <= 0 || width <= 0) {
            return;
        }
        int trackX = x + width - SCROLLBAR_W;
        ctx.fillRoundedRect(trackX, contentY, SCROLLBAR_W, contentH, SCROLLBAR_W / 2,
                ColorUtil.withAlpha(theme.border(), 0.35f));

        int thumbH = thumbHeight(contentH, maxScroll);
        int thumbY = contentY + thumbOffset(contentH, maxScroll, thumbH);
        boolean hot = draggingThumb
                || (mouseX >= trackX - 2 && mouseX < trackX + SCROLLBAR_W + 2
                    && mouseY >= contentY && mouseY < contentY + contentH);
        ctx.fillRoundedRect(trackX, thumbY, SCROLLBAR_W, thumbH, SCROLLBAR_W / 2,
                hot ? theme.accent() : ColorUtil.withAlpha(theme.foregroundMuted(), 0.8f));
    }

    private int thumbHeight(int contentH, int maxScroll) {
        float visibleFraction = contentH / (float) (contentH + maxScroll);
        return Math.max(SCROLLBAR_MIN_THUMB, Math.round(contentH * visibleFraction));
    }

    private int thumbOffset(int contentH, int maxScroll, int thumbH) {
        if (maxScroll <= 0) {
            return 0;
        }
        return Math.round((contentH - thumbH) * (scrollY / (float) maxScroll));
    }

    private int maxScrollFor(ClickGuiBrowseLayout layout) {
        int contentH = Math.max(0, layout.gridH() - TAB_H - CARD_GAP);
        int cols = Math.max(1, layout.columns());
        int rows = (filteredModules().size() + cols - 1) / cols;
        return Math.max(0, rows * (layout.cardH() + CARD_GAP) - contentH);
    }

    private void renderCategoryPills(RenderContext ctx, Theme theme, int x, int y, int width,
                                     double mouseX, double mouseY) {
        int tabX = x;
        for (ModuleCategory cat : ModuleCategory.values()) {
            int tw = tabWidth(ctx, cat);
            if (tabX + tw > x + width) {
                break;
            }
            boolean active = cat == activeCategory && !isSearching();
            boolean hover = mouseX >= tabX && mouseX < tabX + tw && mouseY >= y && mouseY < y + TAB_H;
            int fill = active
                    ? ColorUtil.withAlpha(cat.accent(), 0.9f)
                    : (hover ? theme.surfaceElevated() : theme.backgroundLight());
            ctx.fillRoundedRect(tabX, y, tw, TAB_H, TAB_H / 2, fill);
            int textColor = active ? 0xFFFFFFFF : (hover ? theme.foreground() : theme.foregroundMuted());
            int textY = y + (TAB_H - ctx.uiFontHeight()) / 2 + 1;
            GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, cat.displayName(), tw - TAB_PAD + 8),
                    tabX + TAB_PAD / 2, textY, textColor);
            tabX += tw + ValarisDesign.SPACE_XS;
        }
    }

    private void renderCard(RenderContext ctx, Theme theme, Module module, int x, int y,
                            int cardW, int cardH, double mouseX, double mouseY) {
        boolean hasOptions = hasOptions(module);
        boolean sel = module == selected;
        boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH;
        int[] options = optionsRect(cardW, cardH);
        boolean optionsHover = hasOptions && hover && localHit(mouseX, mouseY, x, y, options);

        // The card body is the enable/disable target, so it only highlights when the
        // cursor is on the body - not when it is over the separate OPTIONS row.
        UiChrome.cardElevated(ctx, theme, x, y, cardW, cardH, sel || (hover && !optionsHover));

        int[] pill = pillRect(cardW, cardH);
        int fontH = ctx.uiFontHeight();
        int titleY = hasOptions
                ? options[1] - 3 - 2 - fontH
                : pill[1] - 4 - fontH;
        titleY = Math.max(2, titleY);

        // Icon fills the space above the title, scaled down on short cards.
        int iconZoneH = Math.max(0, titleY - 4);
        float iconScale = Math.min(ICON_SCALE_MAX, Math.max(1f, iconZoneH / (float) Math.max(1, fontH)));
        String icon = module.category().icon();
        int iconW = ctx.smoothTextWidth(icon, iconScale);
        int iconH = Math.round(fontH * iconScale);
        if (iconZoneH >= fontH) {
            ctx.drawSmoothText(icon, x + (cardW - iconW) / 2, y + 2 + (iconZoneH - iconH) / 2,
                    ColorUtil.withAlpha(module.category().accent(), 0.95f), iconScale);
        }

        String title = GuiLayout.trimToWidth(ctx, module.name(), cardW - SIDE_PAD * 2);
        GuiLayout.label(ctx, title, x + (cardW - GuiLayout.labelWidth(ctx, title)) / 2,
                y + titleY, theme.foreground());

        // OPTIONS row is omitted entirely for modules that expose no settings.
        if (hasOptions) {
            ctx.fillRect(x + SIDE_PAD, y + options[1] - 3, Math.max(0, cardW - SIDE_PAD * 2), 1,
                    ColorUtil.withAlpha(theme.border(), 0.6f));
            if (optionsHover || sel) {
                ctx.fillRoundedRect(x + 2, y + options[1], Math.max(0, cardW - 4), options[3],
                        ValarisDesign.RADIUS_SM,
                        ColorUtil.withAlpha(theme.accent(), optionsHover ? 0.22f : 0.12f));
            }
            int optionsColor = optionsHover || sel ? theme.foreground() : theme.foregroundMuted();
            int optionsTextY = y + options[1] + (options[3] - fontH) / 2 + 1;
            String gear = "⚙";
            int gearW = GuiLayout.labelWidth(ctx, gear);
            String optionsLabel = GuiLayout.trimToWidth(ctx, "OPTIONS",
                    cardW - SIDE_PAD * 2 - gearW - 4);
            GuiLayout.label(ctx, optionsLabel, x + SIDE_PAD, optionsTextY, optionsColor);
            GuiLayout.label(ctx, gear, x + cardW - SIDE_PAD - gearW, optionsTextY, optionsColor);
        }

        UiChrome.pillButton(ctx, theme, x + pill[0], y + pill[1], pill[2], pill[3], module.isEnabled());
        String pillLabel = GuiLayout.trimToWidth(ctx,
                module.isEnabled() ? "ENABLED" : "DISABLED", pill[2] - 4);
        GuiLayout.label(ctx, pillLabel,
                x + pill[0] + (pill[2] - GuiLayout.labelWidth(ctx, pillLabel)) / 2,
                y + pill[1] + (pill[3] - fontH) / 2 + 1,
                module.isEnabled() ? 0xFFFFFFFF : theme.foregroundMuted());
    }

    private static boolean hasOptions(Module module) {
        return !module.settings().isEmpty();
    }

    /** Card-relative {x, y, w, h} of the ENABLED/DISABLED pill. Package-visible for tests. */
    static int[] pillRect(int cardW, int cardH) {
        int h = Math.min(PILL_H, Math.max(8, cardH / 5));
        return new int[]{SIDE_PAD, cardH - 5 - h, Math.max(0, cardW - SIDE_PAD * 2), h};
    }

    /** Card-relative {x, y, w, h} of the OPTIONS row. */
    static int[] optionsRect(int cardW, int cardH) {
        int[] pill = pillRect(cardW, cardH);
        int h = Math.min(OPTIONS_H, Math.max(8, cardH / 6));
        return new int[]{0, pill[1] - 2 - h, cardW, h};
    }

    private static boolean localHit(double mouseX, double mouseY, int cardX, int cardY, int[] rect) {
        return mouseX >= cardX + rect[0] && mouseX < cardX + rect[0] + rect[2]
                && mouseY >= cardY + rect[1] && mouseY < cardY + rect[1] + rect[3];
    }

    public boolean mousePressed(RenderContext ctx, ClickGuiBrowseLayout layout,
                                double mouseX, double mouseY, int button) {
        int x = layout.gridX();
        int y = layout.gridY();
        int width = layout.gridW();
        if (width <= 0 || layout.gridH() <= 0) {
            return false;
        }

        if (mouseY >= y && mouseY < y + TAB_H) {
            int tabX = x;
            for (ModuleCategory cat : ModuleCategory.values()) {
                int tw = tabWidth(ctx, cat);
                if (tabX + tw > x + width) {
                    break;
                }
                if (mouseX >= tabX && mouseX < tabX + tw) {
                    activeCategory = cat;
                    targetScrollY = 0;
                    scrollY = 0;
                    return true;
                }
                tabX += tw + ValarisDesign.SPACE_XS;
            }
        }

        int cardW = layout.cardW();
        int cardH = layout.cardH();
        int cols = Math.max(1, layout.columns());
        int contentY = y + TAB_H + CARD_GAP;
        int contentH = Math.max(0, layout.gridH() - TAB_H - CARD_GAP);
        int rowSpan = cardH + CARD_GAP;
        int maxScroll = maxScrollFor(layout);

        // Scrollbar thumb drag - the touchpad-friendly path.
        if (button == 0 && maxScroll > 0) {
            int trackX = x + width - SCROLLBAR_W;
            if (mouseX >= trackX - 2 && mouseX < trackX + SCROLLBAR_W + 2
                    && mouseY >= contentY && mouseY < contentY + contentH) {
                int thumbH = thumbHeight(contentH, maxScroll);
                int thumbY = contentY + thumbOffset(contentH, maxScroll, thumbH);
                if (mouseY < thumbY || mouseY >= thumbY + thumbH) {
                    // Click on empty track - jump so the thumb centres on the cursor.
                    float f = (float) (mouseY - contentY - thumbH / 2.0) / Math.max(1, contentH - thumbH);
                    targetScrollY = GuiLayout.clamp(Math.round(f * maxScroll), 0, maxScroll);
                }
                draggingThumb = true;
                dragAnchorY = mouseY;
                dragAnchorScroll = targetScrollY;
                return true;
            }
        }

        List<Module> list = filteredModules();
        for (int i = 0; i < list.size(); i++) {
            int cx = x + (i % cols) * (cardW + CARD_GAP);
            int cy = contentY + (i / cols) * rowSpan - Math.round(scrollY);
            if (mouseX >= cx && mouseX < cx + cardW && mouseY >= cy && mouseY < cy + cardH) {
                Module module = list.get(i);
                if (button == 2) {
                    favorites.toggle(module.id());
                } else if (hasOptions(module)
                        && localHit(mouseX, mouseY, cx, cy, optionsRect(cardW, cardH))) {
                    // Only the OPTIONS row opens settings; clicking it again closes them.
                    selected = module == selected ? null : module;
                } else {
                    // The rest of the card - including the pill - is the on/off switch.
                    module.toggle();
                }
                return true;
            }
        }

        // Empty space inside the grid: start a click-drag pan (touchpad friendly).
        if (button == 0 && maxScroll > 0
                && mouseX >= x && mouseX < x + width
                && mouseY >= contentY && mouseY < contentY + contentH) {
            draggingGrid = true;
            dragMoved = false;
            dragAnchorY = mouseY;
            dragAnchorScroll = targetScrollY;
            return true;
        }
        return false;
    }

    /** Continues a scrollbar-thumb or grid pan drag. */
    public void mouseDragged(double mouseY, ClickGuiBrowseLayout layout) {
        int maxScroll = maxScrollFor(layout);
        if (maxScroll <= 0) {
            return;
        }
        int contentH = Math.max(0, layout.gridH() - TAB_H - CARD_GAP);
        if (draggingThumb) {
            int thumbH = thumbHeight(contentH, maxScroll);
            int travel = Math.max(1, contentH - thumbH);
            float delta = (float) (mouseY - dragAnchorY) / travel * maxScroll;
            targetScrollY = GuiLayout.clamp(Math.round(dragAnchorScroll + delta), 0, maxScroll);
            scrollY = targetScrollY;
        } else if (draggingGrid) {
            // Natural drag: content follows the finger.
            float delta = (float) (dragAnchorY - mouseY);
            if (Math.abs(delta) > 2) {
                dragMoved = true;
            }
            targetScrollY = GuiLayout.clamp(Math.round(dragAnchorScroll + delta), 0, maxScroll);
        }
    }

    public void mouseReleased() {
        draggingThumb = false;
        draggingGrid = false;
        dragMoved = false;
    }

    public boolean isDragging() {
        return draggingThumb || draggingGrid;
    }

    /** Keyboard scrolling so the grid is reachable without any pointer gesture. */
    public boolean keyPressed(int glfwKey, ClickGuiBrowseLayout layout) {
        int maxScroll = maxScrollFor(layout);
        if (maxScroll <= 0) {
            return false;
        }
        int page = Math.max(24, layout.gridH() - TAB_H - CARD_GAP);
        int step = layout.cardH() + CARD_GAP;
        Integer target = switch (glfwKey) {
            case 264 -> Math.round(targetScrollY) + step;   // Down
            case 265 -> Math.round(targetScrollY) - step;   // Up
            case 267 -> Math.round(targetScrollY) + page;   // Page Down
            case 266 -> Math.round(targetScrollY) - page;   // Page Up
            case 268 -> 0;                                  // Home
            case 269 -> maxScroll;                          // End
            default -> null;
        };
        if (target == null) {
            return false;
        }
        targetScrollY = GuiLayout.clamp(target, 0, maxScroll);
        return true;
    }

    public boolean mouseScrolled(double amount, ClickGuiBrowseLayout layout) {
        int contentH = Math.max(0, layout.gridH() - TAB_H - CARD_GAP);
        int cols = Math.max(1, layout.columns());
        int rowSpan = layout.cardH() + CARD_GAP;
        int rows = (filteredModules().size() + cols - 1) / cols;
        int maxScroll = Math.max(0, rows * rowSpan - contentH);
        targetScrollY = GuiLayout.clamp(Math.round(targetScrollY - (float) amount * 24f), 0, maxScroll);
        return true;
    }

    /** Pre-selects a module for headless GUI tests. */
    void selectForTests(Module module) {
        selected = module;
        if (module != null) {
            activeCategory = module.category();
        }
    }

    private List<Module> filteredModules() {
        if (isSearching()) {
            return modules.search(searchQuery);
        }
        return modules.byCategory(activeCategory);
    }

    private static int tabWidth(RenderContext ctx, ModuleCategory cat) {
        if (ctx == null) {
            return cat.displayName().length() * 6 + TAB_PAD;
        }
        return ctx.uiTextWidth(cat.displayName()) + TAB_PAD;
    }
}
