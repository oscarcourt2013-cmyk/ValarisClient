package dev.stellarclient.core.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.config.ConfigManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Named configuration profiles (default, pvp, survival, ...).
 *
 * <p>Each profile is one JSON file under {@code config/stellarclient/profiles/}
 * holding the full state of every {@link ConfigManager} binding. Which profile
 * is active persists in {@code config/stellarclient/state.json}.</p>
 */
public final class ProfileManager {

    public static final String DEFAULT_PROFILE = "default";

    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_-]{1,32}");

    private final ConfigManager configManager;
    private final Path profilesDir;
    private final Path stateFile;

    private String activeProfile = DEFAULT_PROFILE;
    private long lastBridgePollMs;
    private long lastProfileMtime = -1;

    public ProfileManager(ConfigManager configManager, Path clientConfigDir) {
        this.configManager = configManager;
        this.profilesDir = clientConfigDir.resolve("profiles");
        this.stateFile = clientConfigDir.resolve("state.json");
    }

    /** Restores the last active profile. @return {@code true} if no profile file existed yet */
    public boolean loadInitial() {
        Path file = profileFile(DEFAULT_PROFILE);
        boolean freshInstall = !Files.isRegularFile(file);
        this.activeProfile = readPersistedProfileName();
        Path active = profileFile(activeProfile);
        configManager.loadFrom(active);
        lastProfileMtime = readMtime(active);
        return freshInstall;
    }

    /**
     * Reloads cosmetics when the launcher bridge rewrites the active profile file.
     * Called from the client tick â€” cheap mtime check every 2s.
     */
    public void pollExternalBridgeChanges() {
        long now = System.currentTimeMillis();
        if (now - lastBridgePollMs < 2_000L) {
            return;
        }
        lastBridgePollMs = now;
        Path file = profileFile(activeProfile);
        long mtime = readMtime(file);
        if (mtime < 0 || mtime == lastProfileMtime) {
            return;
        }
        lastProfileMtime = mtime;
        configManager.reloadSection(file, "cosmetics");
        configManager.reloadSection(file, "theme");
        configManager.reloadSection(file, "modules");
    }

    /** Saves the active profile to disk. */
    public void saveActive() {
        Path file = profileFile(activeProfile);
        // Absorb launcher bridge writes (theme / cosmetics / modules) before we overwrite.
        long mtime = readMtime(file);
        if (mtime > lastProfileMtime) {
            lastProfileMtime = mtime;
            configManager.reloadSection(file, "cosmetics");
            configManager.reloadSection(file, "theme");
            configManager.reloadSection(file, "modules");
        }
        configManager.saveTo(file);
        lastProfileMtime = readMtime(file);
    }

    /**
     * Saves the current profile, then loads {@code name} (created on first
     * save if it never existed).
     *
     * @throws IllegalArgumentException on invalid profile names
     */
    public void switchTo(String name) {
        requireValidName(name);
        if (name.equals(activeProfile)) {
            return;
        }
        saveActive();
        this.activeProfile = name;
        configManager.loadFrom(profileFile(name));
        persistState();
        StellarClient.LOGGER.info("Switched to profile '{}'", name);
    }

    public String activeProfile() {
        return activeProfile;
    }

    /** Profiles present on disk. Scans the directory â€” not a hot path. */
    public List<String> listProfiles() {
        if (!Files.isDirectory(profilesDir)) {
            return List.of(activeProfile);
        }
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.list(profilesDir)) {
            files.map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(".json"))
                    .map(fileName -> fileName.substring(0, fileName.length() - ".json".length()))
                    .sorted()
                    .forEach(names::add);
        } catch (IOException e) {
            StellarClient.LOGGER.error("Failed to list profiles in {}", profilesDir, e);
        }
        if (!names.contains(activeProfile)) {
            names.add(activeProfile);
        }
        return names;
    }

    private Path profileFile(String name) {
        return profilesDir.resolve(name + ".json");
    }

    private static void requireValidName(String name) {
        if (!VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Invalid profile name '" + name + "' (allowed: letters, digits, '-', '_', max 32 chars)");
        }
    }

    private String readPersistedProfileName() {
        if (!Files.isRegularFile(stateFile)) {
            return DEFAULT_PROFILE;
        }
        try {
            JsonObject state = JsonParser
                    .parseString(Files.readString(stateFile, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonElement name = state.get("activeProfile");
            if (name != null && VALID_NAME.matcher(name.getAsString()).matches()) {
                return name.getAsString();
            }
        } catch (IOException | RuntimeException e) {
            StellarClient.LOGGER.error("Failed to read {} â€” using default profile", stateFile, e);
        }
        return DEFAULT_PROFILE;
    }

    private void persistState() {
        JsonObject state = new JsonObject();
        state.addProperty("activeProfile", activeProfile);
        try {
            Files.createDirectories(stateFile.getParent());
            Files.writeString(stateFile, state.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            StellarClient.LOGGER.error("Failed to persist active profile", e);
        }
    }

    private static long readMtime(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return -1;
            }
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return -1;
        }
    }
}
