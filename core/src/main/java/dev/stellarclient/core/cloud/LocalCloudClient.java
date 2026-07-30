package dev.stellarclient.core.cloud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Local filesystem cloud stub — versioned backups under {@code cloud/} until a remote API exists.
 */
public final class LocalCloudClient implements CloudClient {

    private final Path cloudRoot;
    private String accountId = "local";

    public LocalCloudClient(Path cloudRoot) {
        this.cloudRoot = cloudRoot;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId == null || accountId.isBlank() ? "local" : accountId;
    }

    @Override
    public boolean isAuthenticated() {
        return accountId != null;
    }

    @Override
    public Optional<String> accountId() {
        return Optional.ofNullable(accountId);
    }

    @Override
    public void uploadConfig(String profileName, JsonElement config) {
        try {
            Path profileDir = cloudRoot.resolve(safe(profileName));
            Files.createDirectories(profileDir);
            String versionId = Instant.now().toString().replace(':', '-');
            Path file = profileDir.resolve(versionId + ".json");
            Files.writeString(file, config.toString());
            Files.writeString(profileDir.resolve("latest.json"), config.toString());
        } catch (Exception ignored) {
        }
    }

    @Override
    public Optional<JsonElement> downloadConfig(String profileName) {
        try {
            Path latest = cloudRoot.resolve(safe(profileName)).resolve("latest.json");
            if (!Files.isRegularFile(latest)) {
                return Optional.empty();
            }
            return Optional.of(com.google.gson.JsonParser.parseString(Files.readString(latest)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<VersionEntry> listVersions(String profileName) {
        List<VersionEntry> entries = new ArrayList<>();
        Path profileDir = cloudRoot.resolve(safe(profileName));
        if (!Files.isDirectory(profileDir)) {
            return entries;
        }
        try (Stream<Path> stream = Files.list(profileDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().equals("latest.json"))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .forEach(p -> {
                        String id = p.getFileName().toString().replace(".json", "");
                        entries.add(new VersionEntry(id, id, p.toFile().lastModified()));
                    });
        } catch (Exception ignored) {
        }
        return entries;
    }

    @Override
    public Optional<JsonElement> restoreVersion(String profileName, String versionId) {
        try {
            Path file = cloudRoot.resolve(safe(profileName)).resolve(safe(versionId) + ".json");
            if (!Files.isRegularFile(file)) {
                return Optional.empty();
            }
            JsonElement json = com.google.gson.JsonParser.parseString(Files.readString(file));
            uploadConfig(profileName, json);
            return Optional.of(json);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String safe(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
