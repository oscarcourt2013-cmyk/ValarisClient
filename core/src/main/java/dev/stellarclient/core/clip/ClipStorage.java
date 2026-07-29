package dev.stellarclient.core.clip;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Clip output directory: {@code config/stellarclient/clips/} (read by StellarClient Launcher). */
public final class ClipStorage {

    private final Path clipsDir;
    private final Path tempRoot;

    public ClipStorage(Path modRoot) {
        this.clipsDir = modRoot.resolve("clips");
        this.tempRoot = clipsDir.resolve(".tmp");
        try {
            Files.createDirectories(clipsDir);
        } catch (IOException ignored) {
        }
    }

    public Path clipsDirectory() {
        return clipsDir;
    }

    public Path clipFile(String fileName) {
        return clipsDir.resolve(sanitize(fileName));
    }

    public Path thumbnailFor(Path mp4Path) {
        String base = mp4Path.getFileName().toString();
        int dot = base.lastIndexOf('.');
        String stem = dot > 0 ? base.substring(0, dot) : base;
        return mp4Path.getParent().resolve(stem + ".jpg");
    }

    public Path createTempSessionDir() throws IOException {
        Files.createDirectories(tempRoot);
        Path session = tempRoot.resolve("session-" + UUID.randomUUID());
        Files.createDirectories(session);
        return session;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
