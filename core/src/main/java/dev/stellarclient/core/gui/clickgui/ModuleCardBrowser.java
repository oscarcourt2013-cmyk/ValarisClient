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

/** Card-based module browser: pill category filters + a grid of module cards. */
public final class ModuleCardBrowser {

    public static final int CARD_W = 128;
    public static final int CARD_H = 96;
    public static final int TAB_H = 20;

    private static final float ICON_SCALE = 2.1f;
    private static final int ICON_CENTER_Y = 24;
    private static final int TITLE_Y = 42;
    private static final int DIVIDER_Y = 56;
    private static final int OPTIONS_Y = 60;
    private static final int OPTIONS_H = 16;
    private static final int PILL_Y = 78;
    private static final int PILL_H = 14;
    private static final int SIDE_PAD = 8;
    private static final int TAB_PAD = 22;

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

    public void render(RenderContext ctx, Theme theme, int x, int y, int width, int height,
                       double mouseX, double mouseY) {
        ctx.pushClip(x, y, width, TAB_H);
        renderCategoryPills(ctx, theme, x, y, width, mouseX, mouseY);
        ctx.popClip();

        int contentY = y + TAB_H + PrimeDesign.SPACE_SM;
        int contentH = height - TAB_H - PrimeDesign.SPACE_SM;
        List<Module> list = filteredModules();
        int cols = Math.max(1, width / (CARD_W + PrimeDesign.SPACE_SM));
        int rows = (list.size() + cols - 1) / cols;
        int totalH = rows * (CARD_H + PrimeDesign.SPACE_SM);
        int maxScroll = Math.max(0, totalH - contentH);
        if (targetScrollY > maxScroll) {
            targetScrollY = maxScroll;
        }
        if (scrollY > maxScroll) {
            scrollY = maxScroll;
        }

        ctx.pushClip(x, contentY, width, Math.max(0, contentH));
        int rowSpan = CARD_H + PrimeDesign.SPACE_SM;
        int startRow = rowSpan <= 0 ? 0 : Math.max(0, (int) (scrollY / rowSpan));
        int endRow = rowSpan <= 0 ? rows : Math.min(rows, startRow + (contentH / rowSpan) + 2);
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                if (i >= list.size()) {
                    break;
                }
                Module module = list.get(i);
                int cx = x + col * (CARD_W + PrimeDesign.SPACE_SM);
                int cy = contentY + row * rowSpan - Math.round(scrollY);
                if (cy + CARD_H < contentY || cy > contentY + contentH) {
                    continue;
                }
                renderCard(ctx, theme, module, cx, cy, mouseX, mouseY);
            }
        }
        ctx.popClip();
    }

    private void renderCategoryPills(RenderContext ctx, Theme theme, int x, int y, int width,
                                     double mouseX, double mouseY) {
        int tabX = x;
        for (ModuleCategory cat : ModuleCategory.values()) {
            int tw = tabWidth(ctx, cat);
            if (tabX > x + width) {
                break;
            }
            boolean active = cat == activeCategory && !isSearching();
            boolean hover = mouseX >= tabX && mouseX < tabX + tw && mouseY >= y && mouseY < y + TAB_H;
            int radius = TAB_H / 2;
            int fill = active
                    ? ColorUtil.withAlpha(cat.accent(), 0.9f)
                    : (hover ? theme.surfaceElevated() : theme.backgroundLight());
            ctx.fillRoundedRect(tabX, y, tw, TAB_H, radius, fill);
            int textColor = active ? 0xFFFFFFFF : (hover ? theme.foreground() : theme.foregroundMuted());
            GuiLayout.label(ctx, cat.icon(), tabX + 8, y + (TAB_H - ctx.uiFontHeight()) / 2 + 1, textColor);
            GuiLayout.label(ctx, cat.displayName(), tabX + 20, y + (TAB_H - ctx.uiFontHeight()) / 2 + 1, textColor);
            tabX += tw + PrimeDesign.SPACE_XS;
        }
    }

    private void renderCard(RenderContext ctx, Theme theme, Module module, int x, int y,
                            double mouseX, double mouseY) {
        boolean sel = module == selected;
        boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
        UiChrome.cardElevated(ctx, theme, x, y, CARD_W, CARD_H, sel || hover);

        int accent = module.category().accent();
        String icon = module.category().icon();
        int iconW = ctx.smoothTextWidth(icon, ICON_SCALE);
        int iconH = Math.round(ctx.uiFontHeight() * ICON_SCALE);
        ctx.drawSmoothText(icon, x + (CARD_W - iconW) / 2, y + ICON_CENTER_Y - iconH / 2,
                ColorUtil.withAlpha(accent, 0.95f), ICON_SCALE);

        String title = GuiLayout.trimToWidth(ctx, module.name(), CARD_W - SIDE_PAD * 2);
        int titleW = GuiLayout.labelWidth(ctx, title);
        GuiLayout.label(ctx, title, x + (CARD_W - titleW) / 2, y + TITLE_Y, theme.foreground());

        ctx.fillRect(x + SIDE_PAD, y + DIVIDER_Y, CARD_W - SIDE_PAD * 2, 1,
                ColorUtil.withAlpha(theme.border(), 0.6f));

        boolean optionsHover = hover && localHit(mouseX, mouseY, x, y, optionsRowRect());
        int optionsColor = optionsHover ? theme.foreground() : theme.foregroundMuted();
        GuiLayout.label(ctx, "OPTIONS", x + SIDE_PAD,
                y + OPTIONS_Y + (OPTIONS_H - ctx.uiFontHeight()) / 2 + 1, optionsColor);
        String gear = "⚙";
        int gearW = GuiLayout.labelWidth(ctx, gear);
        GuiLayout.label(ctx, gear, x + CARD_W - SIDE_PAD - gearW,
                y + OPTIONS_Y + (OPTIONS_H - ctx.uiFontHeight()) / 2 + 1, optionsColor);

        int[] pill = pillButtonRect();
        UiChrome.pillButton(ctx, theme, x + pill[0], y + pill[1], pill[2], pill[3], module.isEnabled());
        String pillLabel = module.isEnabled() ? "ENABLED" : "DISABLED";
        int pillLabelW = GuiLayout.labelWidth(ctx, pillLabel);
        int pillTextColor = module.isEnabled() ? 0xFFFFFFFF : theme.foregroundMuted();
        GuiLayout.label(ctx, pillLabel, x + pill[0] + (pill[2] - pillLabelW) / 2,
                y + pill[1] + (pill[3] - ctx.uiFontHeight()) / 2 + 1, pillTextColor);
    }

    private static int[] optionsRowRect() {
        return new int[]{0, OPTIONS_Y, CARD_W, OPTIONS_H};
    }

    /** Card-relative rect of the ENABLED/DISABLED pill button. Package-visible for tests. */
    static int[] pillButtonRect() {
        int w = CARD_W - SIDE_PAD * 2;
        return new int[]{SIDE_PAD, PILL_Y, w, PILL_H};
    }

    private static boolean localHit(double mouseX, double mouseY, int cardX, int cardY, int[] rect) {
        return mouseX >= cardX + rect[0] && mouseX < cardX + rect[0] + rect[2]
                && mouseY >= cardY + rect[1] && mouseY < cardY + rect[1] + rect[3];
    }

    public boolean mousePressed(RenderContext ctx, double mouseX, double mouseY, int x, int y, int width, int height, int button) {
        if (mouseY >= y && mouseY < y + TAB_H) {
            int tabX = x;
            for (ModuleCategory cat : ModuleCategory.values()) {
                int tw = tabWidth(ctx, cat);
                if (tabX > x + width) {
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
        List<Module> list = filteredModules();
        int cols = Math.max(1, width / (CARD_W + PrimeDesign.SPACE_SM));
        int contentY = y + TAB_H + PrimeDesign.SPACE_SM;
        int rowSpan = CARD_H + PrimeDesign.SPACE_SM;
        for (int i = 0; i < list.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (CARD_W + PrimeDesign.SPACE_SM);
            int cy = contentY + row * rowSpan - Math.round(scrollY);
            if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                Module module = list.get(i);
                if (button == 2) {
                    favorites.toggle(module.id());
                } else if (button == 0 && localHit(mouseX, mouseY, cx, cy, pillButtonRect())) {
                    module.toggle();
                } else {
                    selected = module;
                }
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double amount, int x, int y, int width, int height) {
        int contentH = height - TAB_H - PrimeDesign.SPACE_SM;
        List<Module> list = filteredModules();
        int cols = Math.max(1, width / (CARD_W + PrimeDesign.SPACE_SM));
        int rows = (list.size() + cols - 1) / cols;
        int totalH = rows * (CARD_H + PrimeDesign.SPACE_SM);
        int maxScroll = Math.max(0, totalH - contentH);
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
