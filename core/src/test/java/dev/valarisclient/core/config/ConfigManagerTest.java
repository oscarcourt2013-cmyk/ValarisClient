package dev.valarisclient.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    private static final class StringBinding implements ConfigBinding {
        private final String key;
        String value;

        StringBinding(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String configKey() {
            return key;
        }

        @Override
        public JsonElement saveConfig() {
            return new JsonPrimitive(value);
        }

        @Override
        public void loadConfig(JsonElement element) {
            this.value = element.getAsString();
        }
    }

    @Test
    void saveThenLoadRoundTrips(@TempDir Path dir) {
        ConfigManager manager = new ConfigManager();
        StringBinding binding = new StringBinding("greeting", "hello");
        manager.register(binding);

        Path file = dir.resolve("profiles").resolve("default.json");
        manager.saveTo(file);
        assertTrue(Files.isRegularFile(file));

        binding.value = "changed";
        manager.loadFrom(file);
        assertEquals("hello", binding.value);
    }

    @Test
    void loadMissingFileKeepsDefaults(@TempDir Path dir) {
        ConfigManager manager = new ConfigManager();
        StringBinding binding = new StringBinding("greeting", "default");
        manager.register(binding);

        manager.loadFrom(dir.resolve("nope.json"));
        assertEquals("default", binding.value);
    }

    @Test
    void corruptFileKeepsDefaults(@TempDir Path dir) throws Exception {
        ConfigManager manager = new ConfigManager();
        StringBinding binding = new StringBinding("greeting", "default");
        manager.register(binding);

        Path file = dir.resolve("broken.json");
        Files.writeString(file, "{ not json !!!");
        manager.loadFrom(file);
        assertEquals("default", binding.value);
    }

    @Test
    void brokenSectionDoesNotAffectOthers(@TempDir Path dir) throws Exception {
        ConfigManager manager = new ConfigManager();
        StringBinding first = new StringBinding("first", "a");
        StringBinding second = new StringBinding("second", "b");
        manager.register(first);
        manager.register(second);

        Path file = dir.resolve("partial.json");
        Files.writeString(file, "{\"first\": {\"unexpected\": \"object\"}, \"second\": \"loaded\"}");
        manager.loadFrom(file);

        assertEquals("a", first.value);
        assertEquals("loaded", second.value);
    }

    @Test
    void duplicateKeyIsRejected() {
        ConfigManager manager = new ConfigManager();
        manager.register(new StringBinding("dup", "x"));
        assertThrows(IllegalArgumentException.class, () -> manager.register(new StringBinding("dup", "y")));
    }
}
