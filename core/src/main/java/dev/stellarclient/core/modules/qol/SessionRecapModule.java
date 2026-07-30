package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.event.PlayerDeathEvent;
import dev.stellarclient.core.event.WorldLeaveEvent;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;
import dev.stellarclient.core.social.SocialService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Session recap on disconnect or world leave. */
public final class SessionRecapModule extends Module {

    private static final long CRASH_SCAN_INTERVAL_MS = 5_000;

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private final SocialService social;

    private final BooleanSetting uploadCrashReports =
            addSetting(new BooleanSetting("upload-crash-reports", "Upload crash reports",
                    "Send new crash-reports to the Prime backend when social is connected", false));

    private int deaths;
    private int pearlsThrown;
    private long sessionStart;
    private long lastCrashScanMs;
    private final Set<String> uploadedCrashFiles = new HashSet<>();

    public SessionRecapModule(MinecraftAdapter adapter, NotificationManager notifications,
                              SocialService social) {
        super("session-recap", "Session Recap", "Summary when leaving a world", ModuleCategory.QOL);
        this.adapter = adapter;
        this.notifications = notifications;
        this.social = social;
        listen(PlayerDeathEvent.class, event -> deaths++);
        listen(WorldLeaveEvent.class, event -> showRecap());
        listen(ClientTickEvent.class, event -> scanCrashReports());
    }

    @Override
    protected void onEnable() {
        deaths = 0;
        pearlsThrown = 0;
        sessionStart = System.currentTimeMillis();
        lastCrashScanMs = 0;
    }

    /** Called from pearl cooldown tracking via adapter heuristic on disable. */
    public void trackPearlThrow() {
        pearlsThrown++;
    }

    private void showRecap() {
        long minutes = Math.max(1, (System.currentTimeMillis() - sessionStart) / 60_000L);
        notifications.info("Session Recap",
                minutes + " min · " + deaths + " deaths · ~" + pearlsThrown + " pearls");
    }

    private void scanCrashReports() {
        if (!isEnabled() || !uploadCrashReports.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastCrashScanMs < CRASH_SCAN_INTERVAL_MS) {
            return;
        }
        lastCrashScanMs = now;
        Path crashDir = adapter.gameDirectory().resolve("crash-reports");
        if (!Files.isDirectory(crashDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(crashDir, "*.txt")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                if (uploadedCrashFiles.contains(name)) {
                    continue;
                }
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (text.isBlank()) {
                    continue;
                }
                if (social.uploadCrash(name, text)) {
                    uploadedCrashFiles.add(name);
                }
            }
        } catch (IOException ignored) {
        }
    }
}
