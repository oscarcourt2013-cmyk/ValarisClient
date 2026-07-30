package dev.valarisclient.core.theme;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThemeManagerTest {

    @Test
    void registersFiveSharedThemes() {
        ThemeManager themes = new ThemeManager();
        assertEquals(5, themes.all().size());
        assertEquals("valaris-crimson", themes.active().id());
    }

    @Test
    void normalizesLegacyIds() {
        assertEquals("valaris-crimson", ThemeManager.normalizeId("valaris-dark"));
        assertEquals("valaris-midnight", ThemeManager.normalizeId("valaris-light"));
    }

    @Test
    void loadsLegacyConfig() {
        ThemeManager themes = new ThemeManager();
        JsonObject json = new JsonObject();
        json.addProperty("active", "valaris-dark");
        themes.loadConfig(json);
        assertEquals("valaris-crimson", themes.active().id());

        json.addProperty("active", "valaris-aurora");
        themes.loadConfig(json);
        assertEquals("valaris-aurora", themes.active().id());
        assertTrue(themes.trySetActive("valaris-midnight"));
        assertEquals("valaris-midnight", themes.active().id());
    }
}
