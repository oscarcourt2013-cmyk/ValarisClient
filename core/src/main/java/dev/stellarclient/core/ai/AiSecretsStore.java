package dev.stellarclient.core.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local Groq API key storage â€” never synced with profiles/cloud.
 * File: {@code config/StellarClient/secrets.json}
 */
public final class AiSecretsStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("Prime AI");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ENV_KEY = "GROQ_API_KEY";

    private final Path secretsFile;
    private String groqApiKey = "";

    public AiSecretsStore(Path configRoot) {
        this.secretsFile = configRoot.resolve("secrets.json");
        reload();
    }

    public Path secretsFile() {
        return secretsFile;
    }

    public synchronized String groqApiKey() {
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            return groqApiKey.trim();
        }
        String env = System.getenv(ENV_KEY);
        return env == null ? "" : env.trim();
    }

    public synchronized boolean hasKey() {
        return !groqApiKey().isBlank();
    }

    public synchronized void setGroqApiKey(String key) {
        this.groqApiKey = key == null ? "" : key.trim();
        save();
    }

    public synchronized void reload() {
        if (!Files.isRegularFile(secretsFile)) {
            return;
        }
        try {
            String raw = Files.readString(secretsFile, StandardCharsets.UTF_8);
            JsonObject json = GSON.fromJson(raw, JsonObject.class);
            if (json != null && json.has("groqApiKey") && json.get("groqApiKey").isJsonPrimitive()) {
                groqApiKey = json.get("groqApiKey").getAsString();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load AI secrets: {}", e.getMessage());
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(secretsFile.getParent());
            JsonObject json = new JsonObject();
            json.addProperty("groqApiKey", groqApiKey == null ? "" : groqApiKey);
            Files.writeString(secretsFile, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Failed to save AI secrets: {}", e.getMessage());
        }
    }
}
