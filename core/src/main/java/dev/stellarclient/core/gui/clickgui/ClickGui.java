package dev.stellarclient.core.gui.clickgui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.config.ConfigBinding;
import dev.stellarclient.core.gui.FavoritesManager;
import dev.stellarclient.core.cloud.CloudSyncManager;
import dev.stellarclient.core.cosmetics.CosmeticManager;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.menu.MainMenuRenderer;
import dev.stellarclient.core.gui.menu.OnboardingManager;
import dev.stellarclient.core.gui.menu.OnboardingScreen;
import dev.stellarclient.core.gui.component.ColorPickerWidget;
import dev.stellarclient.core.gui.component.TextInputField;
import dev.stellarclient.core.module.ColorSetting;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.module.Setting;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;
import dev.stellarclient.core.profile.ProfileManager;
import dev.stellarclient.core.gui.TooltipRenderer;
import dev.stellarclient.core.i18n.PrimeLang;
import dev.stellarclient.core.gui.clickgui.ModuleCardBrowser;
import dev.stellarclient.core.gui.menu.ConfigurationsMenuRenderer;
import dev.stellarclient.core.gui.menu.CosmeticsMenuRenderer;
import dev.stellarclient.core.gui.menu.SettingsMenuRenderer;
import dev.stellarclient.core.keybind.KeybindManager;

import dev.stellarclient.core.util.ColorUtil;
import dev.stellarclient.core.util.Easing;

import java.util.ArrayList;
import java.util.List;

/**
 * Prime ClickGUI with main menu, favorites, category panels and search.
 */
public final class ClickGui implements ConfigBinding {

    private static final int SEARCH_MIN_WIDTH = 90;
    private static final int SEARCH_HEIGHT = 14;
    private static final int MENU_WIDTH = 140;
    private static final int MENU_ROW = 18;
    private static final int MENU_PADDING = 12;

    private final ModuleManager modules;
    private final ThemeManager themes;
    private final FavoritesManager favorites;
    private final MinecraftAdapter adapter;
    private final OnboardingManager onboarding;
    private final CloudSyncManager cloudSync;
    private final CosmeticManager cosmetics;
    private final ProfileManager profiles;
    private final KeybindManager keybinds;
    private final TooltipRenderer tooltips;
    private final MainMenuRenderer mainMenu = new MainMenuRenderer();
    private final ModuleCardBrowser cardBrowser;
    private final ClickGuiTopBar topBar = new ClickGuiTopBar();
    private final ProfileSidebar sidebar = new ProfileSidebar();
    private final BottomDock bottomDock = new BottomDock();
    private final SettingsMenuRenderer settingsMenu = new SettingsMenuRenderer();
    private final CosmeticsMenuRenderer cosmeticsMenu = new CosmeticsMenuRenderer();
    private final ConfigurationsMenuRenderer configurationsMenu = new ConfigurationsMenuRenderer();
    private Panel selectedModulePanel;

    private TextInputField stringEditor;
    private StringSetting editingString;
    private ColorPickerWidget colorPicker;
    private ColorSetting editingColor;
    private int editorX;
    private int editorY;

    private final List<Panel> panels = new ArrayList<>();
    private Panel favoritesPanel;
    private final StringBuilder searchQuery = new StringBuilder();

    private Panel searchPanel;
    private Panel draggingPanel;
    private Panel sliderPanel;
    private Setting sliderSetting;

    private int screenWidth = 800;
    private int screenHeight = 600;
    private int fontHeight = 9;
    private RenderContext textMetrics;
    private ClickGuiView view = ClickGuiView.MAIN_MENU;
    private float openFade;
    private float menuSlide;
    private Runnable onboardingCompleteHandler;

    public ClickGui(ModuleManager modules, ThemeManager themes,
                    FavoritesManager favorites, MinecraftAdapter adapter,
                    OnboardingManager onboarding, CloudSyncManager cloudSync,
                    CosmeticManager cosmetics, ProfileManager profiles,
                    KeybindManager keybinds, TooltipRenderer tooltips) {
        this.modules = modules;
        this.themes = themes;
        this.favorites = favorites;
        this.adapter = adapter;
        this.onboarding = onboarding;
        this.cloudSync = cloudSync;
        this.cosmetics = cosmetics;
        this.profiles = profiles;
        this.keybinds = keybinds;
        this.tooltips = tooltips;
        this.cardBrowser = new ModuleCardBrowser(modules, favorites);
        float x = 8;
        for (ModuleCategory category : ModuleCategory.values()) {
            panels.add(new Panel(category.displayName(), modules.byCategory(category),
                    favorites, x, 8));
            x += Panel.WIDTH + 8;
        }
        favoritesPanel = new Panel(PrimeLang.get("prime.gui.clickgui.favorites", "Favorites"),
                favorites.resolve(modules), favorites, 8, 8);
    }

    public void setOnboardingCompleteHandler(Runnable handler) {
        this.onboardingCompleteHandler = handler;
    }

    public void onOpen() {
        searchQuery.setLength(0);
        searchPanel = null;
        draggingPanel = null;
        sliderPanel = null;
        sliderSetting = null;
        closeEditors();
        view = onboarding.completed() ? ClickGuiView.MAIN_MENU : ClickGuiView.ONBOARDING;
        openFade = 0f;
        menuSlide = -12f;
    }

    private void closeEditors() {
        stringEditor = null;
        editingString = null;
        colorPicker = null;
        editingColor = null;
    }

    /** For tests: skip the main menu and show category panels. */
    public void showBrowse() {
        view = ClickGuiView.BROWSE;
        openFade = 1f;
        menuSlide = 0f;
    }

    /** Opens ClickGUI on the settings hub (title screen shortcut). */
    public void showSettings() {
        view = ClickGuiView.SETTINGS;
        openFade = 1f;
        menuSlide = 0f;
    }

    /** For tests: select a module card and open its settings panel. */
    public void selectModuleForTests(Module module) {
        cardBrowser.selectForTests(module);
        refreshSelectedPanel();
    }

    /** For tests: interact with a legacy category panel (still persisted in config). */
    public boolean pressCategoryPanel(ModuleCategory category, double mouseX, double mouseY, int button) {
        for (Panel panel : panels) {
            if (panel.title().equals(category.displayName())) {
                return dispatchPress(panel, mouseX, mouseY, button);
            }
        }
        return false;
    }

    /** For tests: drag a legacy category panel header. */
    public void dragCategoryPanel(ModuleCategory category, double mouseX, double mouseY,
                                  int screenW, int screenH) {
        for (Panel panel : panels) {
            if (panel.title().equals(category.displayName())) {
                panel.mouseDragged(mouseX, mouseY, screenW, screenH);
                return;
            }
        }
    }

    public ClickGuiView view() {
        return view;
    }

    public void tick(float deltaSeconds) {
        openFade = Easing.lerp(openFade, 1f, deltaSeconds * 8f);
        if (view == ClickGuiView.MAIN_MENU || view == ClickGuiView.ONBOARDING) {
            menuSlide = Easing.lerp(menuSlide, 0f, deltaSeconds * 10f);
            mainMenu.tick(deltaSeconds);
        }
        for (Panel panel : panels) {
            panel.tick(deltaSeconds);
        }
        if (favoritesPanel != null) {
            favoritesPanel.tick(deltaSeconds);
        }
        if (searchPanel != null) {
            searchPanel.tick(deltaSeconds);
        }
        cardBrowser.tick(deltaSeconds);
        refreshSelectedPanel();
    }

    private void refreshSelectedPanel() {
        Module sel = cardBrowser.selected();
        if (sel == null) {
            selectedModulePanel = null;
            return;
        }
        if (selectedModulePanel == null || !selectedModulePanel.hasModule(sel)) {
            selectedModulePanel = new Panel(sel.name(), List.of(sel), favorites,
                    screenWidth - Panel.WIDTH - 12, 8);
            selectedModulePanel.collapsed = false;
        }
    }

    public void render(RenderContext ctx, double mouseX, double mouseY) {
        this.textMetrics = ctx;
        this.screenWidth = ctx.screenWidth();
        this.screenHeight = ctx.screenHeight();
        this.fontHeight = ctx.fontHeight();
        Theme theme = themes.active();

        if (isSearching() && view != ClickGuiView.BROWSE) {
            renderSearchBar(ctx, theme);
            if (searchPanel != null) {
                searchPanel.render(ctx, theme, mouseX, mouseY);
            }
            return;
        }

        switch (view) {
            case MAIN_MENU -> {
                mainMenu.renderBackground(ctx, theme, openFade);
                int px = mainMenu.panelX(screenWidth);
                int py = mainMenu.panelY(screenWidth, screenHeight, menuSlide);
                mainMenu.renderPanel(ctx, theme, px, py, adapter.playerName(),
                        PrimeDesign.VERSION, mouseX, mouseY, menuSlide);
            }
            case ONBOARDING -> renderOnboarding(ctx, theme, mouseX, mouseY);
            case SETTINGS -> {
                renderScrim(ctx, theme);
                settingsMenu.render(ctx, theme, themes, profiles, cloudSync, adapter, keybinds,
                        screenWidth, screenHeight, mouseX, mouseY);
            }
            case COSMETICS -> {
                renderScrim(ctx, theme);
                cosmeticsMenu.render(ctx, theme, cosmetics, screenWidth, screenHeight, mouseX, mouseY);
            }
            case CONFIGURATIONS -> {
                renderScrim(ctx, theme);
                configurationsMenu.render(ctx, theme, cloudSync, profiles, screenWidth, screenHeight, mouseX, mouseY);
            }
            case FAVORITES -> {
                renderScrim(ctx, theme);
                renderSearchBar(ctx, theme);
                refreshFavoritesPanel();
                favoritesPanel.render(ctx, theme, mouseX, mouseY);
            }
            case BROWSE -> {
                renderScrim(ctx, theme);
                ClickGuiBrowseLayout layout = ClickGuiBrowseLayout.compute(screenWidth, screenHeight,
                        selectedModulePanel != null);
                cardBrowser.setSearchQuery(searchQuery.toString());
                topBar.render(ctx, theme, layout, view, searchQuery.toString(), mouseX, mouseY);
                if (layout.showSidebar()) {
                    sidebar.render(ctx, theme, layout, profiles, mouseX, mouseY);
                }
                cardBrowser.render(ctx, theme, layout, mouseX, mouseY);
                bottomDock.render(ctx, theme, layout, mouseX, mouseY);
                if (selectedModulePanel != null) {
                    selectedModulePanel.render(ctx, theme, mouseX, mouseY);
                }
            }
        }
        renderEditors(ctx, theme);
        tooltips.render(ctx, theme);
    }

    /**
     * Full-screen dim behind the ClickGUI content views so panels and cards read
     * clearly instead of competing with the world showing through.
     */
    private void renderScrim(RenderContext ctx, Theme theme) {
        float fade = Easing.easeOutCubic(openFade);
        ctx.fillRect(0, 0, screenWidth, screenHeight,
                ColorUtil.withAlpha(theme.overlay(), fade));
        // Slightly deeper at the very top and bottom so the bar and dock sit on
        // a darker base than the middle of the screen.
        int band = Math.max(8, screenHeight / 8);
        int edge = ColorUtil.withAlpha(0xFF000000, 0.35f * fade);
        int clear = ColorUtil.withAlpha(0xFF000000, 0f);
        ctx.fillGradientVertical(0, 0, screenWidth, band, edge, clear);
        ctx.fillGradientVertical(0, screenHeight - band, screenWidth, band, clear, edge);
    }

    private void renderEditors(RenderContext ctx, Theme theme) {
        if (stringEditor != null) {
            int[] pos = GuiLayout.clampPopup(editorX, editorY, 140, PrimeDesign.INPUT_HEIGHT,
                    screenWidth, screenHeight);
            editorX = pos[0];
            editorY = pos[1];
            stringEditor.render(ctx, theme, editorX, editorY, 140);
        }
        if (colorPicker != null) {
            int[] pos = GuiLayout.clampPopup(editorX, editorY,
                    ColorPickerWidget.WIDTH, ColorPickerWidget.HEIGHT, screenWidth, screenHeight);
            editorX = pos[0];
            editorY = pos[1];
            colorPicker.render(ctx, theme, editorX, editorY);
        }
    }

    private void renderOnboarding(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        mainMenu.renderBackground(ctx, theme, openFade);
        OnboardingScreen.render(ctx, theme, onboarding, screenWidth, screenHeight, menuSlide, mouseX, mouseY);
    }

    private void completeOnboarding() {
        if (onboardingCompleteHandler != null) {
            onboardingCompleteHandler.run();
        }
        view = ClickGuiView.MAIN_MENU;
    }

    private void renderSimpleMenu(RenderContext ctx, Theme theme, String title, String... lines) {
        mainMenu.renderBackground(ctx, theme, openFade);
        int w = 260;
        int h = 80 + lines.length * 14;
        int x = (screenWidth - w) / 2;
        int y = (screenHeight - h) / 2;
        ctx.fillRect(x, y, w, h, theme.background());
        ctx.fillRect(x, y, w, 2, theme.accent());
        ctx.drawText(title, x + 12, y + 12, theme.accent(), true);
        int ly = y + 32;
        for (String line : lines) {
            ctx.drawText(line, x + 12, ly, theme.foregroundMuted(), true);
            ly += 14;
        }
    }

    private void renderSearchBar(RenderContext ctx, Theme theme) {
        String label = searchQuery.isEmpty()
                ? PrimeLang.get("prime.gui.clickgui.search.placeholder", "Type to search...")
                : searchQuery.toString();
        int labelColor = searchQuery.isEmpty() ? theme.foregroundMuted() : theme.foreground();
        int maxWidth = ctx.screenWidth() - 32;
        int width = Math.min(maxWidth, Math.max(SEARCH_MIN_WIDTH, GuiLayout.labelWidth(ctx, label) + 16));
        int x = (ctx.screenWidth() - width) / 2;
        int y = ctx.screenHeight() - SEARCH_HEIGHT - 8;
        ctx.fillRoundedRect(x, y, width, SEARCH_HEIGHT, PrimeDesign.RADIUS_MD, theme.backgroundLight());
        ctx.fillGradientHorizontal(x + 2, y + SEARCH_HEIGHT - 2, width - 4, 2,
                theme.accent(), dev.stellarclient.core.util.ColorUtil.withAlpha(theme.accent(), 0.05f));
        GuiLayout.label(ctx, GuiLayout.trimToWidth(ctx, label, width - 12),
                x + 6, y + (SEARCH_HEIGHT - ctx.uiFontHeight()) / 2 + 1, labelColor);
    }

    public boolean mousePressed(double mouseX, double mouseY, int button) {
        if (colorPicker != null && colorPicker.mousePressed(mouseX, mouseY, editorX, editorY, button)) {
            return true;
        }
        if (stringEditor != null && stringEditor.hit(mouseX, mouseY, editorX, editorY, 140)) {
            stringEditor.setFocused(true);
            return true;
        }
        if (view == ClickGuiView.ONBOARDING) {
            if (OnboardingScreen.mousePressed(onboarding, mouseX, mouseY, screenWidth, screenHeight,
                    menuSlide, button)) {
                if (onboarding.completed()) {
                    completeOnboarding();
                }
                return true;
            }
            return true;
        }
        if (view == ClickGuiView.MAIN_MENU) {
            return handleMainMenuPress(mouseX, mouseY);
        }
        if (view == ClickGuiView.SETTINGS) {
            return settingsMenu.mousePressed(textMetrics, mouseX, mouseY, screenWidth, screenHeight, themes, keybinds, adapter);
        }
        if (view == ClickGuiView.COSMETICS) {
            return cosmeticsMenu.mousePressed(textMetrics, mouseX, mouseY, screenWidth, screenHeight, cosmetics);
        }
        if (view == ClickGuiView.CONFIGURATIONS) {
            return configurationsMenu.mousePressed(mouseX, mouseY, button, cloudSync, profiles,
                    screenWidth, screenHeight);
        }
        if (isSearching() && view != ClickGuiView.BROWSE) {
            return dispatchPress(searchPanel, mouseX, mouseY, button);
        }
        if (view == ClickGuiView.FAVORITES) {
            refreshFavoritesPanel();
            return dispatchPress(favoritesPanel, mouseX, mouseY, button);
        }
        if (view == ClickGuiView.BROWSE) {
            return handleBrowsePress(mouseX, mouseY, button);
        }
        return false;
    }

    private boolean handleBrowsePress(double mouseX, double mouseY, int button) {
        ClickGuiBrowseLayout layout = ClickGuiBrowseLayout.compute(screenWidth, screenHeight,
                selectedModulePanel != null);

        if (selectedModulePanel != null && dispatchPress(selectedModulePanel, mouseX, mouseY, button)) {
            return true;
        }
        if (topBar.hitClose(textMetrics, layout, mouseX, mouseY)) {
            adapter.closeCurrentScreen();
            return true;
        }
        ClickGuiView tab = topBar.hitTab(textMetrics, layout, mouseX, mouseY);
        if (tab != null) {
            if (tab != ClickGuiView.BROWSE) {
                view = tab;
            }
            return true;
        }
        if (layout.showSidebar() && sidebar.mousePressed(layout, profiles, adapter, mouseX, mouseY, button)) {
            return true;
        }
        BottomDock.Action dockAction = bottomDock.hit(layout, mouseX, mouseY);
        if (dockAction != null) {
            handleDockAction(dockAction);
            return true;
        }
        return cardBrowser.mousePressed(textMetrics, layout, mouseX, mouseY, button);
    }

    private void handleDockAction(BottomDock.Action action) {
        switch (action) {
            case HUD_EDITOR -> adapter.openHudEditor();
            case COSMETICS -> view = ClickGuiView.COSMETICS;
            case CONFIGURATIONS -> view = ClickGuiView.CONFIGURATIONS;
            case SETTINGS -> view = ClickGuiView.SETTINGS;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (view == ClickGuiView.BROWSE) {
            ClickGuiBrowseLayout layout = ClickGuiBrowseLayout.compute(screenWidth, screenHeight,
                    selectedModulePanel != null);
            return cardBrowser.mouseScrolled(verticalAmount, layout);
        }
        if (view == ClickGuiView.COSMETICS) {
            return cosmeticsMenu.scroll(verticalAmount);
        }
        if (view == ClickGuiView.SETTINGS) {
            return settingsMenu.scroll(verticalAmount);
        }
        return false;
    }

    private boolean handleMainMenuPress(double mouseX, double mouseY) {
        int btn = mainMenu.hitButton(mouseX, mouseY, screenWidth, screenHeight, menuSlide);
        if (btn < 0) {
            return false;
        }
        if (btn == 0) {
            adapter.closeCurrentScreen();
            return true;
        }
        if (btn == 2) {
            adapter.openHudEditor();
            return true;
        }
        ClickGuiView next = mainMenu.viewForButton(btn);
        if (next != null) {
            view = next;
        }
        return true;
    }

    private void openStringEditor(StringSetting setting, int x, int y) {
        editingString = setting;
        stringEditor = new TextInputField(setting.get(), setting.name(), 48);
        stringEditor.setFocused(true);
        int[] pos = GuiLayout.clampPopup(x, y, 140, PrimeDesign.INPUT_HEIGHT, screenWidth, screenHeight);
        editorX = pos[0];
        editorY = pos[1];
        colorPicker = null;
        editingColor = null;
    }

    private void openColorEditor(ColorSetting setting, int x, int y) {
        editingColor = setting;
        colorPicker = new ColorPickerWidget();
        colorPicker.load(setting.get());
        int[] pos = GuiLayout.clampPopup(x, y, ColorPickerWidget.WIDTH, ColorPickerWidget.HEIGHT,
                screenWidth, screenHeight);
        editorX = pos[0];
        editorY = pos[1];
        stringEditor = null;
        editingString = null;
    }

    private boolean dispatchPress(Panel panel, double mouseX, double mouseY, int button) {
        Panel.Hit hit = panel.mousePressed(mouseX, mouseY, button);
        if (hit.isMiss()) {
            return false;
        }
        if (hit.isStringEdit()) {
            openStringEditor(hit.stringSetting(), (int) mouseX, (int) mouseY);
            return true;
        }
        if (hit.isColorEdit()) {
            openColorEditor(hit.colorSetting(), (int) mouseX, (int) mouseY);
            return true;
        }
        this.draggingPanel = panel;
        if (hit.isSlider()) {
            this.sliderPanel = panel;
            this.sliderSetting = hit.slider();
        }
        return true;
    }

    public void mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (view == ClickGuiView.BROWSE && cardBrowser.isDragging()) {
            cardBrowser.mouseDragged(mouseY, ClickGuiBrowseLayout.compute(
                    screenWidth, screenHeight, selectedModulePanel != null));
            return;
        }
        if (colorPicker != null) {
            colorPicker.mouseDragged(mouseX, mouseY, editorX, editorY);
            if (editingColor != null) {
                editingColor.set(colorPicker.selectedArgb());
            }
            return;
        }
        if (sliderSetting != null) {
            sliderPanel.dragSlider(sliderSetting, mouseX);
            return;
        }
        if (draggingPanel != null) {
            draggingPanel.mouseDragged(mouseX, mouseY, screenWidth, screenHeight);
        }
    }

    public void mouseReleased() {
        cardBrowser.mouseReleased();
        if (draggingPanel != null) {
            draggingPanel.mouseReleased();
        }
        draggingPanel = null;
        sliderPanel = null;
        sliderSetting = null;
        if (colorPicker != null) {
            colorPicker.mouseReleased();
        }
    }

    public boolean charTyped(char character) {
        if (stringEditor != null && stringEditor.charTyped(character)) {
            if (editingString != null) {
                editingString.set(stringEditor.text());
            }
            return true;
        }
        if (view == ClickGuiView.MAIN_MENU || view == ClickGuiView.ONBOARDING) {
            return false;
        }
        if (view == ClickGuiView.BROWSE && sidebar.charTyped(character)) {
            return true;
        }
        if (character < ' ') {
            return false;
        }
        searchQuery.append(character);
        if (view != ClickGuiView.BROWSE) {
            rebuildSearchPanel();
        }
        return true;
    }

    public boolean keyPressed(int glfwKey) {
        if (stringEditor != null && stringEditor.keyPressed(glfwKey)) {
            if (editingString != null) {
                editingString.set(stringEditor.text());
            }
            if (glfwKey == 256) {
                closeEditors();
            }
            return true;
        }
        if (view == ClickGuiView.SETTINGS && settingsMenu.capturingKey()) {
            return settingsMenu.captureKey(glfwKey, keybinds);
        }
        if (view == ClickGuiView.BROWSE && sidebar.keyPressed(glfwKey, profiles)) {
            return true;
        }
        if (view == ClickGuiView.BROWSE && cardBrowser.keyPressed(glfwKey,
                ClickGuiBrowseLayout.compute(screenWidth, screenHeight, selectedModulePanel != null))) {
            return true;
        }
        if (glfwKey == 256 && view == ClickGuiView.ONBOARDING) {
            onboarding.skip();
            completeOnboarding();
            return true;
        }
        if (glfwKey == 256 && view != ClickGuiView.MAIN_MENU && view != ClickGuiView.BROWSE) {
            closeEditors();
            view = ClickGuiView.MAIN_MENU;
            return true;
        }
        if (view == ClickGuiView.CONFIGURATIONS
                && configurationsMenu.keyPressed(glfwKey, cloudSync, profiles)) {
            return true;
        }
        if (view == ClickGuiView.SETTINGS && settingsMenu.keyPressed(glfwKey)) {
            return true;
        }
        if (glfwKey == 256 && view == ClickGuiView.BROWSE && selectedModulePanel != null) {
            selectedModulePanel = null;
            cardBrowser.selectForTests(null);
            return true;
        }
        if (glfwKey == 256 && colorPicker != null) {
            closeEditors();
            return true;
        }
        if (view == ClickGuiView.MAIN_MENU) {
            return false;
        }
        if (glfwKey == 259 && !searchQuery.isEmpty()) {
            searchQuery.setLength(searchQuery.length() - 1);
            rebuildSearchPanel();
            return true;
        }
        if (glfwKey == 256 && isSearching()) {
            searchQuery.setLength(0);
            searchPanel = null;
            return true;
        }
        return false;
    }

    private boolean isSearching() {
        return !searchQuery.isEmpty();
    }

    private void rebuildSearchPanel() {
        if (searchQuery.isEmpty()) {
            searchPanel = null;
            return;
        }
        List<Module> results = modules.search(searchQuery.toString());
        searchPanel = new Panel(PrimeLang.get("prime.gui.clickgui.search.panel", "Search (%d)", results.size()),
                results, favorites,
                searchPanel != null ? searchPanel.x : 8, 8);
    }

    private void refreshFavoritesPanel() {
        favoritesPanel = new Panel(PrimeLang.get("prime.gui.clickgui.favorites", "Favorites"),
                favorites.resolve(modules), favorites, favoritesPanel.x, favoritesPanel.y);
        favoritesPanel.collapsed = false;
    }

    @Override
    public String configKey() {
        return "clickgui";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (Panel panel : panels) {
            JsonObject section = new JsonObject();
            section.addProperty("x", panel.x);
            section.addProperty("y", panel.y);
            section.addProperty("collapsed", panel.collapsed);
            json.add(panel.title(), section);
        }
        JsonObject fav = new JsonObject();
        fav.addProperty("x", favoritesPanel.x);
        fav.addProperty("y", favoritesPanel.y);
        json.add("Favorites", fav);
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        JsonObject json = element.getAsJsonObject();
        for (Panel panel : panels) {
            loadPanelSection(json, panel);
        }
        JsonElement favSection = json.get("Favorites");
        if (favSection != null && favSection.isJsonObject()) {
            JsonObject section = favSection.getAsJsonObject();
            if (section.has("x")) {
                favoritesPanel.x = section.get("x").getAsFloat();
            }
            if (section.has("y")) {
                favoritesPanel.y = section.get("y").getAsFloat();
            }
        }
    }

    private static void loadPanelSection(JsonObject json, Panel panel) {
        JsonElement sectionJson = json.get(panel.title());
        if (sectionJson == null || !sectionJson.isJsonObject()) {
            return;
        }
        JsonObject section = sectionJson.getAsJsonObject();
        if (section.has("x")) {
            panel.x = section.get("x").getAsFloat();
        }
        if (section.has("y")) {
            panel.y = section.get("y").getAsFloat();
        }
        if (section.has("collapsed")) {
            panel.collapsed = section.get("collapsed").getAsBoolean();
        }
    }
}
