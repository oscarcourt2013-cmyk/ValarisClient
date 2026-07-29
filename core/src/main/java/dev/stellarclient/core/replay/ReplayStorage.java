package dev.stellarclient.core.replay;

import com.google.gson.JsonElement;
import dev.stellarclient.core.StellarClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saves and loads replay timelines as JSON files. */
public final class ReplayStorage {

    private final Path replayDir;

    public ReplayStorage(Path modRoot) {
        this.replayDir = modRoot.resolve("replays");
    }

    public void save(ReplaySession session, String name) {
        try {
            Files.createDirectories(replayDir);
            Path file = replayDir.resolve(sanitize(name) + ".json");
            Files.writeString(file, session.toJson().toString());
            StellarClient.LOGGER.info("Saved replay to {}", file);
        } catch (IOException e) {
            StellarClient.LOGGER.error("Failed to save replay {}", name, e);
        }
    }

    public boolean load(ReplaySession session, String name) {
        Path file = replayDir.resolve(sanitize(name) + ".json");
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            JsonElement json = com.google.gson.JsonParser.parseString(Files.readString(file));
            session.loadJson(json);
            return true;
        } catch (IOException e) {
            StellarClient.LOGGER.error("Failed to load replay {}", name, e);
            return false;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
