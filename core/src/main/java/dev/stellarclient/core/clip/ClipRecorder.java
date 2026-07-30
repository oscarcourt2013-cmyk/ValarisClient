package dev.stellarclient.core.clip;

import dev.stellarclient.core.StellarClient;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.notification.NotificationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

/** Captures framebuffer frames in-game and exports MP4 clips for the launcher Media page. */
public final class ClipRecorder {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ClipStorage storage;
    private final NotificationManager notifications;

    private boolean recording;
    private int fps = 30;
    private int maxFrames = 30 * 60;
    private int tickCounter;
    private int frameIndex;
    private Path tempDir;
    private final List<Path> capturedFrames = new ArrayList<>();
    private final AtomicBoolean encoding = new AtomicBoolean(false);

    public ClipRecorder(ClipStorage storage, NotificationManager notifications) {
        this.storage = storage;
        this.notifications = notifications;
    }

    public boolean isRecording() {
        return recording;
    }

    public boolean isEncoding() {
        return encoding.get();
    }

    public int capturedFrameCount() {
        return capturedFrames.size();
    }

    public int maxFrameCount() {
        return maxFrames;
    }

    public void configure(int fps, int maxDurationSeconds) {
        this.fps = Math.max(10, Math.min(60, fps));
        this.maxFrames = this.fps * Math.max(5, Math.min(300, maxDurationSeconds));
    }

    public void start(MinecraftAdapter adapter) {
        if (recording || encoding.get()) {
            return;
        }
        try {
            tempDir = storage.createTempSessionDir();
            capturedFrames.clear();
            frameIndex = 0;
            tickCounter = 0;
            recording = true;
            notifications.info("Clip Recorder", "Recording started");
            StellarClient.LOGGER.info("Clip recording started in {}", tempDir);
        } catch (IOException e) {
            StellarClient.LOGGER.error("Failed to start clip recording", e);
            notifications.error("Clip Recorder", "Could not start recording.");
        }
    }

    public void tick(MinecraftAdapter adapter) {
        if (!recording || encoding.get() || !adapter.isInGame() || adapter.isScreenOpen()) {
            return;
        }

        int tickInterval = Math.max(1, 20 / fps);
        tickCounter++;
        if (tickCounter % tickInterval != 0) {
            return;
        }

        Path framePath = tempDir.resolve(String.format("frame_%06d.png", frameIndex++));
        if (!adapter.captureFrame(framePath)) {
            return;
        }

        capturedFrames.add(framePath);
        if (capturedFrames.size() >= maxFrames) {
            stop(adapter);
        }
    }

    public int elapsedSeconds() {
        return capturedFrames.size() / Math.max(1, fps);
    }

    public int configuredFps() {
        return fps;
    }

    public void stop(MinecraftAdapter adapter) {
        if (!recording) {
            return;
        }
        recording = false;

        if (capturedFrames.isEmpty()) {
            cleanupTemp();
            notifications.warning("Clip Recorder", "No frames captured.");
            return;
        }

        encoding.set(true);
        notifications.info("Clip Recorder", "Encoding clip…");

        List<Path> frames = List.copyOf(capturedFrames);
        Path sessionDir = tempDir;
        int exportFps = fps;

        Thread encoder = new Thread(() -> {
            try {
                String stamp = LocalDateTime.now().format(FILE_STAMP);
                Path mp4 = storage.clipFile(stamp + ".mp4");
                ClipEncoder.encode(sessionDir, mp4, exportFps);

                Path thumb = storage.thumbnailFor(mp4);
                ImageIO.write(ImageIO.read(frames.getFirst().toFile()), "jpg", thumb.toFile());

                adapter.runOnClientThread(() -> notifications.success(
                        "Clip saved",
                        mp4.getFileName().toString() + " — visible in StellarClient Launcher Media"));
                StellarClient.LOGGER.info("Clip exported to {}", mp4);
            } catch (IOException e) {
                StellarClient.LOGGER.error("Clip encoding failed", e);
                adapter.runOnClientThread(() -> notifications.error("Clip Recorder", "Export failed."));
            } finally {
                deleteDirQuietly(sessionDir);
                capturedFrames.clear();
                encoding.set(false);
            }
        }, "prime-clip-encoder");
        encoder.setDaemon(true);
        encoder.start();
    }

    private void cleanupTemp() {
        if (tempDir != null) {
            deleteDirQuietly(tempDir);
        }
        capturedFrames.clear();
        tempDir = null;
    }

    private static void deleteDirQuietly(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
