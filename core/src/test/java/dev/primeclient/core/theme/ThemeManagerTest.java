package dev.stellarclient.core.theme;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThemeManagerTest {

    @Test
    void registersFiveSharedThemes() {
        ThemeManager themes = new ThemeManager();
        assertEquals(5, themes.all().size());
        assertEquals("prime-crimson", themes.active().id());
    }

    @Test
    void normalizesLegacyIds() {
        assertEquals("prime-crimson", ThemeManager.normalizeId("prime-dark"));
        assertEquals("prime-midnight", ThemeManager.normalizeId("prime-light"));
    }

    @Test
    void loadsLegacyConfig() {
        ThemeManager themes = new ThemeManager();
        JsonObject json = new JsonObject();
        json.addProperty("active", "prime-dark");
        themes.loadConfig(json);
        assertEquals("prime-crimson", themes.active().id());

        json.addProperty("active", "prime-aurora");
        themes.loadConfig(json);
        assertEquals("prime-aurora", themes.active().id());
        assertTrue(themes.trySetActive("prime-midnight"));
        assertEquals("prime-midnight", themes.active().id());
    }
}
