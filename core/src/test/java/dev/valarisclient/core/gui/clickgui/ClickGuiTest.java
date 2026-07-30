package dev.valarisclient.core.gui.clickgui;

import com.google.gson.JsonObject;
import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.cloud.CloudSyncManager;
import dev.valarisclient.core.config.ConfigManager;
import dev.valarisclient.core.cosmetics.CosmeticManager;
import dev.valarisclient.core.event.EventBus;
import dev.valarisclient.core.gui.FavoritesManager;
import dev.valarisclient.core.gui.TooltipRenderer;
import dev.valarisclient.core.gui.menu.OnboardingManager;
import dev.valarisclient.core.hud.FakeRenderContext;
import dev.valarisclient.core.keybind.KeybindManager;
import dev.valarisclient.core.module.BooleanSetting;
import dev.valarisclient.core.module.IntSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.module.ModuleManager;
import dev.valarisclient.core.profile.ProfileManager;
import dev.valarisclient.core.theme.ThemeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiTest {

    private static final int SCREEN_W = 800;

    private static final class TestModule extends Module {
        final BooleanSetting flag = addSetting(new BooleanSetting("flag", "Flag", "", false));
        final IntSetting range = addSetting(new IntSetting("range", "Range", "", 5, 0, 10));

        TestModule() {
            super("zoom", "Zoom", "Zooms the camera", ModuleCategory.QOL);
        }
    }

    private static final class StubAdapter implements MinecraftAdapter {
        boolean hudEditorOpened;

        @Override
        public String minecraftVersion() {
            return "test";
        }

        @Override
        public Path gameDirectory() {
            return Path.of(".");
        }

        @Override
        public Path configDirectory() {
            return Path.of(".");
        }

        @Override
        public boolean isInGame() {
            return false;
        }

        @Override
        public boolean isScreenOpen() {
            return false;
        }

        @Override
        public boolean isKeyDown(int glfwKey) {
            return false;
        }

        @Override
        public boolean isMouseButtonDown(int glfwButton) {
            return false;
        }

        @Override
        public int fps() {
            return 60;
        }

        @Override
        public boolean hasPlayer() {
            return false;
        }

        @Override
        public double playerX() {
            return 0;
        }

        @Override
        public double playerY() {
            return 0;
        }

        @Override
        public double playerZ() {
            return 0;
        }

        @Override
        public void runOnClientThread(Runnable task) {
            task.run();
        }

        @Override
        public void openHudEditor() {
            hudEditorOpened = true;
        }

        @Override
        public void openClickGui() {
        }
    }

    private ModuleManager modules;
    private KeybindManager keybinds;
    private FavoritesManager favorites;
    private StubAdapter adapter;
    private CloudSyncManager cloudSync;
    private CosmeticManager cosmetics;
    private ProfileManager profiles;
    private ClickGui gui;
    private TestModule module;

    // Legacy category panel for QoL: PvP, Survival, Performance, then QoL → index 3
    private static final double LEGACY_PANEL_X = 8 + 3 * (Panel.WIDTH + 8);
    private static final double HEADER_Y = 8;

    // The selected-module settings panel docks where the browse layout says it does.
    private static final ClickGuiBrowseLayout PANEL_LAYOUT =
            ClickGuiBrowseLayout.compute(SCREEN_W, 600);
    private static final double PANEL_X = PANEL_LAYOUT.settingsPanelX();
    private static final double PANEL_HEADER_Y = PANEL_LAYOUT.settingsPanelY();
    private static final double FIRST_ROW_Y = PANEL_HEADER_Y + Panel.HEADER_HEIGHT;

    @BeforeEach
    void setUp() {
        keybinds = new KeybindManager();
        modules = new ModuleManager(new EventBus(), keybinds);
        module = modules.register(new TestModule());
        favorites = new FavoritesManager();
        adapter = new StubAdapter();
        OnboardingManager onboarding = new OnboardingManager();
        onboarding.skip();
        ConfigManager configManager = new ConfigManager();
        cloudSync = new CloudSyncManager(
                new dev.valarisclient.core.cloud.LocalCloudClient(java.nio.file.Path.of("cloud-test")),
                configManager,
                new dev.valarisclient.core.notification.NotificationManager());
        cosmetics = new CosmeticManager();
        profiles = new ProfileManager(configManager, Path.of("profiles-test"));
        gui = new ClickGui(modules, new ThemeManager(), favorites, adapter, onboarding,
                cloudSync, cosmetics, profiles, keybinds, new TooltipRenderer());
        gui.onOpen();
        gui.showBrowse();
        gui.selectModuleForTests(module);
        gui.tick(1f / 20f);
    }

    @Test
    void detailPanelListsSettingsWithoutAnExpandStep() {
        // The header already names the module, so row 0 is the first setting
        // rather than a second copy of the name behind a right-click expand.
        assertFalse(module.flag.get());
        assertTrue(gui.mousePressed(PANEL_X + 10, FIRST_ROW_Y + 5, 0));
        assertTrue(module.flag.get(), "row 0 should be the boolean setting");
        gui.mouseReleased();

        // Row 1 is the int slider; clicking at its far right pins it to max.
        double sliderRowY = FIRST_ROW_Y + Panel.ROW_HEIGHT;
        assertTrue(gui.mousePressed(PANEL_X + Panel.WIDTH - 6, sliderRowY + 5, 0));
        assertEquals(10, module.range.get(), "row 1 should be the int slider");
        gui.mouseReleased();
    }

    @Test
    void legacyCategoryPanelKeepsRowAndExpandBehaviour() {
        // Category / favourites / search panels list many modules, so they keep the
        // module row (click to toggle, right-click to expand its settings).
        double rowY = HEADER_Y + Panel.HEADER_HEIGHT;
        assertTrue(gui.pressCategoryPanel(ModuleCategory.QOL, LEGACY_PANEL_X + 10, rowY + 5, 0));
        assertTrue(module.isEnabled(), "left click on a legacy row toggles the module");
        gui.mouseReleased();

        assertTrue(gui.pressCategoryPanel(ModuleCategory.QOL, LEGACY_PANEL_X + 10, rowY + 5, 1));
        gui.mouseReleased();
        double flagRowY = rowY + Panel.ROW_HEIGHT;
        assertTrue(gui.pressCategoryPanel(ModuleCategory.QOL, LEGACY_PANEL_X + 10, flagRowY + 5, 0));
        assertTrue(module.flag.get(), "expanded legacy panel exposes the setting rows");
    }

    @Test
    void middleClickTogglesFavorite() {
        ClickGuiBrowseLayout layout = ClickGuiBrowseLayout.compute(SCREEN_W, 600);
        int cardX = layout.gridX() + 10;
        int cardY = layout.gridY() + ModuleCardBrowser.TAB_H + ClickGuiBrowseLayout.CARD_GAP + 6;
        gui.render(new FakeRenderContext(SCREEN_W, 600), cardX, cardY);
        assertFalse(favorites.isFavorite("zoom"));
        assertTrue(gui.mousePressed(cardX, cardY, 2));
        assertTrue(favorites.isFavorite("zoom"));
        gui.mouseReleased();
    }

    @Test
    void missReturnsFalse() {
        gui.render(new FakeRenderContext(SCREEN_W, 600), 0, 0);
        assertFalse(gui.mousePressed(600, 300, 0));
    }

    @Test
    void panelHeaderDragMovesPanel() {
        assertTrue(gui.pressCategoryPanel(ModuleCategory.QOL, LEGACY_PANEL_X + 10, HEADER_Y + 5, 0));
        gui.dragCategoryPanel(ModuleCategory.QOL, LEGACY_PANEL_X + 60, HEADER_Y + 45, SCREEN_W, 600);
        gui.mouseReleased();

        JsonObject saved = gui.saveConfig().getAsJsonObject();
        JsonObject qol = saved.getAsJsonObject("QoL");
        assertEquals(LEGACY_PANEL_X + 50, qol.get("x").getAsFloat());
        assertEquals(HEADER_Y + 40, qol.get("y").getAsFloat());
    }

    @Test
    void searchFiltersAndEscapeClearsBeforeClosing() {
        gui.render(new FakeRenderContext(SCREEN_W, 600), 0, 0);
        assertTrue(gui.charTyped('z'));

        ClickGuiBrowseLayout layout = ClickGuiBrowseLayout.compute(SCREEN_W, 600);
        int cardX = layout.gridX();
        int cardY = layout.gridY() + ModuleCardBrowser.TAB_H + ClickGuiBrowseLayout.CARD_GAP;
        int[] pill = ModuleCardBrowser.pillRect(layout.cardW(), layout.cardH());
        double pillX = cardX + pill[0] + pill[2] / 2.0;
        double pillY = cardY + pill[1] + pill[3] / 2.0;

        assertTrue(gui.mousePressed(pillX, pillY, 0));
        assertTrue(module.isEnabled());
        gui.mouseReleased();

        assertTrue(gui.keyPressed(256));
        assertTrue(gui.keyPressed(256));
    }

    @Test
    void configRoundTripsPanelState() {
        gui.pressCategoryPanel(ModuleCategory.QOL, LEGACY_PANEL_X + 10, HEADER_Y + 5, 1);
        JsonObject saved = gui.saveConfig().getAsJsonObject();

        ClickGui fresh = new ClickGui(modules, new ThemeManager(), favorites, adapter,
                new OnboardingManager(), cloudSync, cosmetics, profiles, keybinds, new TooltipRenderer());
        fresh.loadConfig(saved);
        assertTrue(fresh.saveConfig().getAsJsonObject().getAsJsonObject("QoL").get("collapsed").getAsBoolean());
    }

    @Test
    void renderDoesNotThrowHeadless() {
        gui.charTyped('z');
        gui.render(new FakeRenderContext(SCREEN_W, 600), 100, 100);
        gui.keyPressed(256);
        gui.render(new FakeRenderContext(SCREEN_W, 600), 100, 100);
    }

    @Test
    void layoutStaysOnScreenAtEveryGuiScale() {
        // Minecraft's effective GUI canvas is window/guiScale and bottoms out
        // around 320x240, so nothing may extend past the screen edge.
        int[][] sizes = {{320, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {1920, 1080}};
        for (int[] size : sizes) {
            int w = size[0];
            int h = size[1];
            for (boolean selection : new boolean[]{false, true}) {
                ClickGuiBrowseLayout l = ClickGuiBrowseLayout.compute(w, h);
                String at = "at " + w + "x" + h + " selection=" + selection;

                assertTrue(l.topBarX() + l.topBarW() <= w, "top bar overflows " + at);
                assertTrue(l.gridX() + l.gridW() <= w, "grid overflows width " + at);
                assertTrue(l.gridY() + l.gridH() <= h, "grid overflows height " + at);
                assertTrue(l.dockX() >= 0 && l.dockX() + l.dockW() <= w, "dock overflows " + at);
                assertTrue(l.dockY() + l.dockH() <= h, "dock below screen " + at);
                assertTrue(l.sidebarY() + l.sidebarH() <= h, "sidebar overflows height " + at);
                assertTrue(l.settingsPanelX() + Panel.WIDTH <= w, "settings panel overflows " + at);

                // A full row of cards must fit inside the grid's width.
                int rowW = l.columns() * l.cardW() + (l.columns() - 1) * ClickGuiBrowseLayout.CARD_GAP;
                assertTrue(rowW <= l.gridW(), "card row wider than grid " + at);
                assertTrue(l.cardW() > 0 && l.cardH() > 0, "degenerate card size " + at);
                assertTrue(l.columns() >= 1, "no columns " + at);

                // The dock must not overlap the scrollable grid area.
                assertTrue(l.gridY() + l.gridH() <= l.dockY(), "grid overlaps dock " + at);
            }
        }
    }

    /** Centre of card {@code index} in a freshly laid-out grid. */
    private static double[] cardCentre(ClickGuiBrowseLayout l, int index) {
        int col = index % Math.max(1, l.columns());
        int row = index / Math.max(1, l.columns());
        double cx = l.gridX() + col * (l.cardW() + ClickGuiBrowseLayout.CARD_GAP);
        double cy = l.gridY() + ModuleCardBrowser.TAB_H + ClickGuiBrowseLayout.CARD_GAP
                + row * (l.cardH() + ClickGuiBrowseLayout.CARD_GAP);
        return new double[]{cx, cy};
    }

    @Test
    void cardBodyTogglesAndOptionsRowOpensSettings() {
        Module plain = modules.register(new Module("plain", "Plain", "", ModuleCategory.PVP) {
        });
        ClickGuiBrowseLayout l = ClickGuiBrowseLayout.compute(SCREEN_W, 600);
        ModuleCardBrowser browser = new ModuleCardBrowser(modules, favorites);
        FakeRenderContext ctx = new FakeRenderContext(SCREEN_W, 600);

        // PvP is the default category, and "plain" is its only module -> card 0.
        double[] c = cardCentre(l, 0);
        int[] options = ModuleCardBrowser.optionsRect(l.cardW(), l.cardH());
        int[] pill = ModuleCardBrowser.pillRect(l.cardW(), l.cardH());

        // A module with no settings has no OPTIONS row, so a click there still toggles.
        browser.mousePressed(ctx, l, c[0] + options[2] / 2.0, c[1] + options[1] + options[3] / 2.0, 0);
        assertTrue(plain.isEnabled(), "no-settings module should toggle from the OPTIONS band");
        assertEquals(null, browser.selected(), "no-settings module must not open a panel");

        // The pill is part of the body, so it toggles rather than opening settings.
        browser.mousePressed(ctx, l, c[0] + pill[0] + pill[2] / 2.0, c[1] + pill[1] + pill[3] / 2.0, 0);
        assertFalse(plain.isEnabled(), "pill should toggle back off");
        assertEquals(null, browser.selected());

        // A module WITH settings: OPTIONS opens the panel and does not toggle.
        ModuleCardBrowser withSettings = new ModuleCardBrowser(modules, favorites);
        // Point the browser at QoL (where "zoom" lives) without leaving it selected.
        withSettings.selectForTests(module);
        withSettings.selectForTests(null);
        double[] q = cardCentre(l, 0);
        boolean before = module.isEnabled();
        withSettings.mousePressed(ctx, l, q[0] + options[2] / 2.0, q[1] + options[1] + options[3] / 2.0, 0);
        assertEquals(module, withSettings.selected(), "OPTIONS should open the settings panel");
        assertEquals(before, module.isEnabled(), "OPTIONS must not toggle the module");

        // Clicking OPTIONS again closes it.
        withSettings.mousePressed(ctx, l, q[0] + options[2] / 2.0, q[1] + options[1] + options[3] / 2.0, 0);
        assertEquals(null, withSettings.selected(), "OPTIONS should close an open panel");
    }

    @Test
    void gridIsScrollableWithoutAMouseWheel() {
        // Enough modules to overflow one screen of cards.
        for (int i = 0; i < 40; i++) {
            int n = i;
            modules.register(new Module("filler" + n, "Filler " + n, "", ModuleCategory.PVP) {
            });
        }
        ClickGuiBrowseLayout layout = ClickGuiBrowseLayout.compute(SCREEN_W, 400);
        ModuleCardBrowser browser = new ModuleCardBrowser(modules, favorites);
        FakeRenderContext ctx = new FakeRenderContext(SCREEN_W, 400);

        // Keyboard: Page Down / End must move, Home must return to the top.
        assertTrue(browser.keyPressed(267, layout), "Page Down should scroll");
        browser.tick(1f);
        assertTrue(browser.keyPressed(269, layout), "End should scroll");
        browser.tick(1f);
        assertTrue(browser.keyPressed(268, layout), "Home should scroll");
        browser.tick(1f);

        // Click-drag panning on empty grid space, for touchpads without wheel gestures.
        int emptyX = layout.gridX() + layout.gridW() - 20;
        int emptyY = layout.gridY() + layout.gridH() - 4;
        browser.render(ctx, new ThemeManager().active(), layout, -1, -1);
        browser.mousePressed(ctx, layout, emptyX, emptyY, 0);
        if (browser.isDragging()) {
            browser.mouseDragged(emptyY - 40, layout);
            browser.tick(1f);
        }
        browser.mouseReleased();
        assertFalse(browser.isDragging(), "drag must end on release");
    }

    @Test
    void renderIsSafeAtMinimumCanvasSize() {
        FakeRenderContext ctx = new FakeRenderContext(320, 240);
        gui.render(ctx, 10, 10);
        assertEquals(0, ctx.clipDepth());
        assertTrue(ctx.fillCalls > 0);
    }

    @Test
    void searchWithNoResultsKeepsClipStackBalanced() {
        for (char c : "zzzz".toCharArray()) {
            assertTrue(gui.charTyped(c));
        }
        FakeRenderContext ctx = new FakeRenderContext(SCREEN_W, 600);
        gui.render(ctx, 100, 100);
        assertEquals(0, ctx.clipDepth());
    }
}
