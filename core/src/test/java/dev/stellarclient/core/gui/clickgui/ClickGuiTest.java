package dev.stellarclient.core.gui.clickgui;

import com.google.gson.JsonObject;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.cloud.CloudSyncManager;
import dev.stellarclient.core.config.ConfigManager;
import dev.stellarclient.core.cosmetics.CosmeticManager;
import dev.stellarclient.core.event.EventBus;
import dev.stellarclient.core.gui.FavoritesManager;
import dev.stellarclient.core.gui.TooltipRenderer;
import dev.stellarclient.core.gui.menu.OnboardingManager;
import dev.stellarclient.core.hud.FakeRenderContext;
import dev.stellarclient.core.keybind.KeybindManager;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.profile.ProfileManager;
import dev.stellarclient.core.theme.ThemeManager;
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

    // Legacy category panel for QoL: PvP, Survival, Performance, then QoL â†’ index 3
    private static final double LEGACY_PANEL_X = 8 + 3 * (Panel.WIDTH + 8);
    private static final double PANEL_X = SCREEN_W - Panel.WIDTH - 12;
    private static final double HEADER_Y = 8;
    private static final double FIRST_ROW_Y = HEADER_Y + 18;

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
                new dev.stellarclient.core.cloud.LocalCloudClient(java.nio.file.Path.of("cloud-test")),
                configManager,
                new dev.stellarclient.core.notification.NotificationManager());
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
    void leftClickOnRowTogglesModule() {
        assertTrue(gui.mousePressed(PANEL_X + 10, FIRST_ROW_Y + 5, 0));
        assertTrue(module.isEnabled());
        gui.mouseReleased();

        assertTrue(gui.mousePressed(PANEL_X + 10, FIRST_ROW_Y + 5, 0));
        assertFalse(module.isEnabled());
    }

    @Test
    void rightClickExpandsAndSettingRowsWork() {
        assertTrue(gui.mousePressed(PANEL_X + 10, FIRST_ROW_Y + 5, 1));
        gui.mouseReleased();

        double flagRowY = FIRST_ROW_Y + 16;
        assertTrue(gui.mousePressed(PANEL_X + 10, flagRowY + 5, 0));
        assertTrue(module.flag.get());
        gui.mouseReleased();

        double sliderRowY = flagRowY + 16;
        assertTrue(gui.mousePressed(PANEL_X + Panel.WIDTH - 6, sliderRowY + 5, 0));
        assertEquals(10, module.range.get());
        gui.mouseReleased();
    }

    @Test
    void middleClickTogglesFavorite() {
        ClickGuiBrowseLayout layout = ClickGuiBrowseLayout.compute(SCREEN_W, 600, true);
        int cardX = layout.gridX() + 10;
        int cardY = layout.gridY() + ModuleCardBrowser.TAB_H + 4 + 10;
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

        ClickGuiBrowseLayout layout = ClickGuiBrowseLayout.compute(SCREEN_W, 600, true);
        int cardX = layout.gridX();
        int cardY = layout.gridY() + ModuleCardBrowser.TAB_H + 4;
        int[] pill = ModuleCardBrowser.pillButtonRect();
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
    void searchWithNoResultsKeepsClipStackBalanced() {
        for (char c : "zzzz".toCharArray()) {
            assertTrue(gui.charTyped(c));
        }
        FakeRenderContext ctx = new FakeRenderContext(SCREEN_W, 600);
        gui.render(ctx, 100, 100);
        assertEquals(0, ctx.clipDepth());
    }
}
