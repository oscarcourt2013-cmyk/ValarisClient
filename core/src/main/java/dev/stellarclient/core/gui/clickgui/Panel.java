package dev.stellarclient.core.gui.clickgui;

import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.gui.FavoritesManager;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.ColorSetting;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.EnumSetting;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.Setting;
import dev.stellarclient.core.i18n.PrimeLang;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.Easing;

import java.util.List;

/** One draggable ClickGUI category panel. */
final class Panel {

    static final int WIDTH = 160;
    static final int HEADER_HEIGHT = 18;
    static final int ROW_HEIGHT = 16;
    private static final int PADDING = 6;
    private static final int SETTING_INDENT = 10;

    private final String title;
    private final List<Module> modules;
    private final FavoritesManager favorites;

    float x;
    float y;
    boolean collapsed;
    private Module expanded;

    private boolean draggingHeader;
    private float grabOffsetX;
    private float grabOffsetY;
    private float collapseProgress = 1f;

    Panel(String title, List<Module> modules, FavoritesManager favorites, float x, float y) {
        this.title = title;
        this.modules = modules;
        this.favorites = favorites;
        this.x = x;
        this.y = y;
        this.collapseProgress = collapsed ? 0f : 1f;
    }

    String title() {
        return title;
    }

    boolean hasModule(Module module) {
        return modules.size() == 1 && modules.get(0) == module;
    }

    void tick(float deltaSeconds) {
        float target = collapsed ? 0f : 1f;
        collapseProgress = Easing.lerp(collapseProgress, target, deltaSeconds * 12f);
    }

    int height() {
        return HEADER_HEIGHT + Math.round(rowCount() * ROW_HEIGHT * collapseProgress);
    }

    private int rowCount() {
        int count = modules.size();
        if (expanded != null && modules.contains(expanded)) {
            count += expanded.settings().size();
        }
        return count;
    }

    void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        int px = Math.round(x);
        int py = Math.round(y);
        int totalH = height();

        UiChrome.glassPanel(ctx, theme, px, py, WIDTH, totalH);
        UiChrome.flatHeader(ctx, theme, px + 1, py + 1, WIDTH - 2, HEADER_HEIGHT);
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, title, WIDTH - PADDING * 2),
                px + PADDING, py + (HEADER_HEIGHT - ctx.uiFontHeight()) / 2 + 1, theme.foreground());

        if (collapseProgress <= 0.01f) {
            return;
        }

        int bodyH = totalH - HEADER_HEIGHT;
        if (bodyH > 0) {
            ctx.pushClip(px, py + HEADER_HEIGHT, WIDTH, bodyH);

            int visibleRows = Math.round(rowCount() * collapseProgress);
            int rowY = py + HEADER_HEIGHT;
            int drawn = 0;
            for (Module module : modules) {
                if (drawn >= visibleRows) {
                    break;
                }
                renderModuleRow(ctx, theme, module, px, rowY, mouseX, mouseY);
                rowY += ROW_HEIGHT;
                drawn++;

                if (module == expanded) {
                    for (Setting setting : module.settings()) {
                        if (drawn >= visibleRows) {
                            break;
                        }
                        ctx.fillRect(px, rowY, WIDTH, ROW_HEIGHT, theme.background());
                        ctx.fillRect(px + SETTING_INDENT - 4, rowY, 1, ROW_HEIGHT, theme.accent() & 0x60FFFFFF);
                        renderSetting(ctx, theme, setting, px, rowY);
                        rowY += ROW_HEIGHT;
                        drawn++;
                    }
                }
            }
            ctx.popClip();
        }
    }

    private void renderModuleRow(RenderContext ctx, Theme theme, Module module, int px, int rowY,
                                 double mouseX, double mouseY) {
        boolean hovered = contains(mouseX, mouseY, px, rowY, WIDTH, ROW_HEIGHT);
        ctx.fillRect(px, rowY, WIDTH, ROW_HEIGHT, hovered ? theme.backgroundLight() : theme.background());

        int textY = rowY + (ROW_HEIGHT - ctx.uiFontHeight()) / 2 + 1;
        int cursorX = px + WIDTH - PADDING;

        String star = favorites.isFavorite(module.id()) ? "â˜…" : "â˜†";
        int starW = GuiLayout.labelWidth(ctx, star);
        cursorX -= starW;
        GuiLayout.label(ctx, star, cursorX, textY,
                favorites.isFavorite(module.id()) ? theme.accent() : theme.foregroundMuted());

        if (!module.settings().isEmpty()) {
            cursorX -= 4;
            String expand = module == expanded ? "âˆ’" : "+";
            int expandW = GuiLayout.labelWidth(ctx, expand);
            cursorX -= expandW;
            GuiLayout.label(ctx, expand, cursorX, textY, theme.foregroundMuted());
        }

        int nameColor = module.isEnabled() ? theme.accent() : theme.foreground();
        int nameMaxW = Math.max(0, cursorX - 4 - px - PADDING);
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, module.name(), nameMaxW),
                px + PADDING, textY, nameColor);
    }

    private void renderSetting(RenderContext ctx, Theme theme, Setting setting, int px, int rowY) {
        int textY = rowY + (ROW_HEIGHT - ctx.uiFontHeight()) / 2 + 1;
        int textX = px + SETTING_INDENT;
        switch (setting) {
            case BooleanSetting bool -> {
                int boxSize = 8;
                int boxX = px + WIDTH - PADDING - boxSize;
                int nameMaxW = boxX - textX - 4;
                GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, setting.name(), nameMaxW),
                        textX, textY, theme.foreground());
                int boxY = rowY + (ROW_HEIGHT - boxSize) / 2;
                ctx.fillRect(boxX, boxY, boxSize, boxSize, theme.backgroundLight());
                if (bool.get()) {
                    ctx.fillRect(boxX + 2, boxY + 2, boxSize - 4, boxSize - 4, theme.accent());
                }
            }
            case IntSetting number -> renderSlider(ctx, theme, setting.name(), String.valueOf(number.get()),
                    (number.get() - number.min()) / (float) (number.max() - number.min()), px, rowY, textX, textY);
            case DoubleSetting number -> renderSlider(ctx, theme, setting.name(), String.format("%.1f", number.get()),
                    (float) ((number.get() - number.min()) / (number.max() - number.min())), px, rowY, textX, textY);
            case EnumSetting<?> mode -> {
                String value = PrimeLang.enumValue(mode.get());
                int valueW = GuiLayout.labelWidth(ctx, value);
                GuiLayout.label(ctx, value, px + WIDTH - PADDING - valueW, textY, theme.accent());
                int nameMaxW = WIDTH - SETTING_INDENT - PADDING - valueW - 4;
                GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, setting.name(), nameMaxW),
                        textX, textY, theme.foreground());
            }
            case ColorSetting color -> {
                int swatch = 8;
                int swatchX = px + WIDTH - PADDING - swatch;
                int nameMaxW = swatchX - textX - 4;
                GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, setting.name(), nameMaxW),
                        textX, textY, theme.foreground());
                ctx.fillRect(swatchX, rowY + (ROW_HEIGHT - swatch) / 2, swatch, swatch, color.get());
            }
            case StringSetting text -> {
                int valueMaxW = WIDTH - SETTING_INDENT - PADDING - 4;
                String value = GuiLayout.trimToWidth(ctx, text.get(), valueMaxW / 2);
                int valueW = GuiLayout.labelWidth(ctx, value);
                GuiLayout.label(ctx, value, px + WIDTH - PADDING - valueW, textY, theme.foregroundMuted());
                int nameMaxW = WIDTH - SETTING_INDENT - PADDING - valueW - 4;
                GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, setting.name(), nameMaxW),
                        textX, textY, theme.foreground());
            }
        }
    }

    private void renderSlider(RenderContext ctx, Theme theme, String name, String value, float fraction,
                              int px, int rowY, int textX, int textY) {
        int valueW = GuiLayout.labelWidth(ctx, value);
        GuiLayout.label(ctx, value, px + WIDTH - PADDING - valueW, textY, theme.foregroundMuted());
        int nameMaxW = WIDTH - SETTING_INDENT - PADDING - valueW - 4;
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, name, nameMaxW), textX, textY, theme.foreground());

        int barX = sliderBarX(px);
        int barWidth = sliderBarWidth();
        int barY = rowY + ROW_HEIGHT - 5;
        ctx.fillRect(barX, barY, barWidth, 2, theme.backgroundLight());
        ctx.fillRect(barX, barY, Math.round(barWidth * fraction), 2, theme.accent());
    }

    private static int sliderBarX(int px) {
        return px + SETTING_INDENT;
    }

    private static int sliderBarWidth() {
        return WIDTH - SETTING_INDENT - PADDING;
    }

    Hit mousePressed(double mouseX, double mouseY, int button) {
        int px = Math.round(x);
        int py = Math.round(y);
        if (contains(mouseX, mouseY, px, py, WIDTH, HEADER_HEIGHT)) {
            if (button == 1) {
                collapsed = !collapsed;
            } else {
                draggingHeader = true;
                grabOffsetX = (float) (mouseX - x);
                grabOffsetY = (float) (mouseY - y);
            }
            return Hit.CONSUMED;
        }
        if (collapseProgress <= 0.01f
                || !contains(mouseX, mouseY, px, py + HEADER_HEIGHT, WIDTH, height() - HEADER_HEIGHT)) {
            return Hit.MISS;
        }

        int rowIndex = (int) ((mouseY - (py + HEADER_HEIGHT)) / ROW_HEIGHT);
        int cursor = 0;
        for (Module module : modules) {
            if (cursor == rowIndex) {
                if (button == 2) {
                    favorites.toggle(module.id());
                } else if (button == 0) {
                    module.toggle();
                } else if (!module.settings().isEmpty()) {
                    expanded = expanded == module ? null : module;
                }
                return Hit.CONSUMED;
            }
            cursor++;
            if (module == expanded) {
                for (Setting setting : module.settings()) {
                    if (cursor == rowIndex) {
                        return settingPressed(setting, mouseX, px);
                    }
                    cursor++;
                }
            }
        }
        return Hit.CONSUMED;
    }

    private Hit settingPressed(Setting setting, double mouseX, int px) {
        switch (setting) {
            case BooleanSetting bool -> bool.toggle();
            case EnumSetting<?> mode -> mode.cycle();
            case IntSetting number -> {
                applySlider(number, mouseX, px);
                return new Hit(number);
            }
            case DoubleSetting number -> {
                applySlider(number, mouseX, px);
                return new Hit(number);
            }
            case ColorSetting color -> {
                return new Hit(color);
            }
            case StringSetting text -> {
                return new Hit(text);
            }
        }
        return Hit.CONSUMED;
    }

    void dragSlider(Setting slider, double mouseX) {
        int px = Math.round(x);
        if (slider instanceof IntSetting number) {
            applySlider(number, mouseX, px);
        } else if (slider instanceof DoubleSetting number) {
            applySlider(number, mouseX, px);
        }
    }

    private void applySlider(IntSetting setting, double mouseX, int px) {
        float fraction = sliderFraction(mouseX, px);
        setting.set(Math.round(setting.min() + fraction * (setting.max() - setting.min())));
    }

    private void applySlider(DoubleSetting setting, double mouseX, int px) {
        float fraction = sliderFraction(mouseX, px);
        setting.set(setting.min() + fraction * (setting.max() - setting.min()));
    }

    private static float sliderFraction(double mouseX, int px) {
        return Math.clamp((float) (mouseX - sliderBarX(px)) / sliderBarWidth(), 0f, 1f);
    }

    void mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (draggingHeader) {
            x = Math.clamp((float) mouseX - grabOffsetX, 0, Math.max(0, screenWidth - WIDTH));
            y = Math.clamp((float) mouseY - grabOffsetY, 0, Math.max(0, screenHeight - height()));
        }
    }

    void mouseReleased() {
        draggingHeader = false;
    }

    private static boolean contains(double mx, double my, int rx, int ry, int rw, int rh) {
        return mx >= rx && mx < rx + rw && my >= ry && my < ry + rh;
    }

    record Hit(Type type, Setting slider, StringSetting stringSetting, ColorSetting colorSetting) {
        enum Type {
            MISS,
            CONSUMED,
            SLIDER,
            STRING_EDIT,
            COLOR_EDIT
        }

        static final Hit MISS = new Hit(Type.MISS, null, null, null);
        static final Hit CONSUMED = new Hit(Type.CONSUMED, null, null, null);

        Hit(Setting slider) {
            this(Type.SLIDER, slider, null, null);
        }

        Hit(StringSetting setting) {
            this(Type.STRING_EDIT, null, setting, null);
        }

        Hit(ColorSetting setting) {
            this(Type.COLOR_EDIT, null, null, setting);
        }

        boolean isMiss() {
            return type == Type.MISS;
        }

        boolean isSlider() {
            return type == Type.SLIDER;
        }

        boolean isStringEdit() {
            return type == Type.STRING_EDIT;
        }

        boolean isColorEdit() {
            return type == Type.COLOR_EDIT;
        }
    }
}
