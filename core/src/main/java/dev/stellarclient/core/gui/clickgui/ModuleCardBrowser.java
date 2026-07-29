package dev.stellarclient.core.gui.clickgui;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.gui.FavoritesManager;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;
import dev.stellarclient.core.util.Easing;

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

    private ModuleCategory activeCategory = ModuleCategory.PVP;
    private String searchQuery = "";
    private Module selected;
    private float scrollY;
    private float targetScrollY;

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
            tabX += tw + PrimeDesign.SPACE_XS;
        }
    }

    private void renderCard(RenderContext ctx, Theme theme, Module module, int x, int y,
                            int cardW, int cardH, double mouseX, double mouseY) {
        boolean sel = module == selected;
        boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH;
        UiChrome.cardElevated(ctx, theme, x, y, cardW, cardH, sel || hover);

        int[] pill = pillRect(cardW, cardH);
        int[] options = optionsRect(cardW, cardH);
        int fontH = ctx.uiFontHeight();
        int dividerY = options[1] - 3;
        int titleY = Math.max(y + 2, dividerY - 2 - fontH) - y;

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

        ctx.fillRect(x + SIDE_PAD, y + dividerY, Math.max(0, cardW - SIDE_PAD * 2), 1,
                ColorUtil.withAlpha(theme.border(), 0.6f));

        boolean optionsHover = hover && localHit(mouseX, mouseY, x, y, options);
        int optionsColor = optionsHover ? theme.foreground() : theme.foregroundMuted();
        int optionsTextY = y + options[1] + (options[3] - fontH) / 2 + 1;
        String gear = "⚙";
        int gearW = GuiLayout.labelWidth(ctx, gear);
        String optionsLabel = GuiLayout.trimToWidth(ctx, "OPTIONS",
                cardW - SIDE_PAD * 2 - gearW - 4);
        GuiLayout.label(ctx, optionsLabel, x + SIDE_PAD, optionsTextY, optionsColor);
        GuiLayout.label(ctx, gear, x + cardW - SIDE_PAD - gearW, optionsTextY, optionsColor);

        UiChrome.pillButton(ctx, theme, x + pill[0], y + pill[1], pill[2], pill[3], module.isEnabled());
        String pillLabel = GuiLayout.trimToWidth(ctx,
                module.isEnabled() ? "ENABLED" : "DISABLED", pill[2] - 4);
        GuiLayout.label(ctx, pillLabel,
                x + pill[0] + (pill[2] - GuiLayout.labelWidth(ctx, pillLabel)) / 2,
                y + pill[1] + (pill[3] - fontH) / 2 + 1,
                module.isEnabled() ? 0xFFFFFFFF : theme.foregroundMuted());
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
                tabX += tw + PrimeDesign.SPACE_XS;
            }
        }

        int cardW = layout.cardW();
        int cardH = layout.cardH();
        int cols = Math.max(1, layout.columns());
        int contentY = y + TAB_H + CARD_GAP;
        int rowSpan = cardH + CARD_GAP;
        List<Module> list = filteredModules();
        for (int i = 0; i < list.size(); i++) {
            int cx = x + (i % cols) * (cardW + CARD_GAP);
            int cy = contentY + (i / cols) * rowSpan - Math.round(scrollY);
            if (mouseX >= cx && mouseX < cx + cardW && mouseY >= cy && mouseY < cy + cardH) {
                Module module = list.get(i);
                if (button == 2) {
                    favorites.toggle(module.id());
                } else if (button == 0 && localHit(mouseX, mouseY, cx, cy, pillRect(cardW, cardH))) {
                    module.toggle();
                } else {
                    selected = module;
                }
                return true;
            }
        }
        return false;
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
