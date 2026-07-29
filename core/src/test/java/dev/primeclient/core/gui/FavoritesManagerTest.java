package dev.stellarclient.core.gui;

import com.google.gson.JsonArray;
import dev.stellarclient.core.event.EventBus;
import dev.stellarclient.core.keybind.KeybindManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FavoritesManagerTest {

    private static final class AlphaModule extends Module {
        AlphaModule() {
            super("alpha", "Alpha", "", ModuleCategory.PVP);
        }
    }

    private static final class BetaModule extends Module {
        BetaModule() {
            super("beta", "Beta", "", ModuleCategory.QOL);
        }
    }

    private ModuleManager modules;
    private FavoritesManager favorites;

    @BeforeEach
    void setUp() {
        modules = new ModuleManager(new EventBus(), new KeybindManager());
        modules.register(new AlphaModule());
        modules.register(new BetaModule());
        favorites = new FavoritesManager();
    }

    @Test
    void toggleAddsAndRemoves() {
        assertFalse(favorites.isFavorite("alpha"));
        favorites.toggle("alpha");
        assertTrue(favorites.isFavorite("alpha"));
        favorites.toggle("alpha");
        assertFalse(favorites.isFavorite("alpha"));
    }

    @Test
    void resolveKeepsOrderAndSkipsMissing() {
        favorites.toggle("beta");
        favorites.toggle("alpha");
        favorites.toggle("missing");

        assertEquals(2, favorites.resolve(modules).size());
        assertEquals("beta", favorites.resolve(modules).get(0).id());
        assertEquals("alpha", favorites.resolve(modules).get(1).id());
    }

    @Test
    void configRoundTrips() {
        favorites.toggle("alpha");
        favorites.toggle("beta");

        JsonArray saved = favorites.saveConfig().getAsJsonArray();
        FavoritesManager fresh = new FavoritesManager();
        fresh.loadConfig(saved);

        assertTrue(fresh.isFavorite("alpha"));
        assertTrue(fresh.isFavorite("beta"));
        assertEquals(2, fresh.resolve(modules).size());
    }
}
