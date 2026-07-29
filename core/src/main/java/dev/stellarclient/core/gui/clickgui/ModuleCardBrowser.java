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

import java.util.ArrayList;
import java.util.List;

/** Card-based module browser with category tabs. */
public final class ModuleCardBrowser {

    public static final int CARD_W = 112;
    public static final int CARD_H = 38;
    public static final int TAB_H = 18;
    public static final int TAB_PAD = 26;

    private static final int TITLE_X = 8;
    private static final int TEXT_PAD = 6;
    private static final int TOGGLE_TOP = 8;

    private final ModuleManager modules;
    private final FavoritesManager favorites;

    private ModuleCategory activeCategory = ModuleCategory.PVP;
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

    public void tick(float deltaSeconds) {
        scrollY = Easing.lerp(scrollY, targetScrollY, deltaSeconds * 14f);
    }

    public void render(RenderContext ctx, Theme theme, int x, int y, int width, int height,
                       double mouseX, double mouseY) {
        ctx.pushClip(x, y, width, TAB_H);
        renderTabs(ctx, theme, x, y, width, mouseX, mouseY);
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

        ctx.pushClip(x, contentY, width, contentH);
        int startRow = Math.max(0, (int) (scrollY / (CARD_H + PrimeDesign.SPACE_SM)));
        int endRow = Math.min(rows, startRow + (contentH / (CARD_H + PrimeDesign.SPACE_SM)) + 2);
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                if (i >= list.size()) {
                    break;
                }
                Module module = list.get(i);
                int cx = x + col * (CARD_W + PrimeDesign.SPACE_SM);
                int cy = contentY + row * (CARD_H + PrimeDesign.SPACE_SM) - Math.round(scrollY);
                if (cy + CARD_H < contentY || cy > contentY + contentH) {
                    continue;
                }
                renderCard(ctx, theme, module, cx, cy, mouseX, mouseY);
            }
        }
        ctx.popClip();
    }

    private void renderTabs(RenderContext ctx, Theme theme, int x, int y, int width,
                            double mouseX, double mouseY) {
        int tabX = x;
        for (ModuleCategory cat : ModuleCategory.values()) {
            int tw = tabWidth(ctx, cat);
            if (tabX > x + width) {
                break;
            }
            boolean active = cat == activeCategory;
            boolean hover = mouseX >= tabX && mouseX < tabX + tw && mouseY >= y && mouseY < y + TAB_H;
            int radius = PrimeDesign.RADIUS_SM;
            int fill = active ? theme.surfaceElevated() : theme.backgroundLight();
            ctx.fillRoundedRect(tabX, y, tw, TAB_H, radius, fill);
            if (active || hover) {
                ctx.fillRect(tabX + 2, y + TAB_H - 2, tw - 4, 1, cat.accent());
            }
            GuiLayout.label(ctx, cat.icon(), tabX + 6, y + 4, cat.accent());
            GuiLayout.label(ctx, cat.displayName(), tabX + 18, y + 4,
                    active ? theme.foreground() : theme.foregroundMuted());
            tabX += tw + PrimeDesign.SPACE_XS;
        }
    }

    private void renderCard(RenderContext ctx, Theme theme, Module module, int x, int y,
                            double mouseX, double mouseY) {
        boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H;
        boolean sel = module == selected;
        UiChrome.cardLite(ctx, theme, x, y, CARD_W, CARD_H, sel || hover);
        if (hover || sel) {
            ctx.fillRect(x + 2, y + CARD_H - 2, CARD_W - 4, 1,
                    ColorUtil.withAlpha(module.category().accent(), sel ? 0.95f : 0.55f));
        }

        ctx.fillRect(x + 2, y + 5, 2, CARD_H - 10, module.category().accent());

        int toggleX = cardToggleX(x);
        int toggleY = cardToggleY(y);
        int textMax = toggleX - (x + TITLE_X) - 4;

        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, module.name(), textMax),
                x + TITLE_X, y + 6, theme.foreground());
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, module.description(), textMax),
                x + TITLE_X, y + 20, theme.foregroundMuted());

        drawCardToggle(ctx, theme, toggleX, toggleY, module.isEnabled());

        if (favorites.isFavorite(module.id())) {
            GuiLayout.label(ctx, "â˜…", x + 6, y + CARD_H - 11, theme.accent());
        }
    }

    private void drawCardToggle(RenderContext ctx, Theme theme, int x, int y, boolean on) {
        int w = PrimeDesign.TOGGLE_WIDTH;
        int h = PrimeDesign.TOGGLE_HEIGHT;
        int radius = h / 2;
        int track = on ? theme.accent() : ColorUtil.withAlpha(theme.backgroundLight(), 0.95f);
        ctx.fillRoundedRect(x, y, w, h, radius, track);
        int knob = h - 4;
        int knobX = on ? x + w - knob - 2 : x + 2;
        ctx.fillRoundedRect(knobX, y + 2, knob, knob, knob / 2, 0xFFFFFFFF);
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
        for (int i = 0; i < list.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (CARD_W + PrimeDesign.SPACE_SM);
            int cy = contentY + row * (CARD_H + PrimeDesign.SPACE_SM) - Math.round(scrollY);
            if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                Module module = list.get(i);
                if (button == 2) {
                    favorites.toggle(module.id());
                } else if (button == 0) {
                    int tx = cardToggleX(cx);
                    int ty = cardToggleY(cy);
                    if (mouseX >= tx && mouseX < tx + PrimeDesign.TOGGLE_WIDTH
                            && mouseY >= ty && mouseY < ty + PrimeDesign.TOGGLE_HEIGHT) {
                        module.toggle();
                    } else {
                        selected = module;
                    }
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
        List<Module> list = new ArrayList<>();
        for (Module m : modules.byCategory(activeCategory)) {
            list.add(m);
        }
        return list;
    }

    private static int cardToggleX(int cardX) {
        return cardX + CARD_W - PrimeDesign.TOGGLE_WIDTH - TEXT_PAD;
    }

    private static int cardToggleY(int cardY) {
        return cardY + TOGGLE_TOP;
    }

    private static int tabWidth(RenderContext ctx, ModuleCategory cat) {
        if (ctx == null) {
            return cat.displayName().length() * 6 + TAB_PAD;
        }
        return ctx.uiTextWidth(cat.displayName()) + TAB_PAD;
    }
}
