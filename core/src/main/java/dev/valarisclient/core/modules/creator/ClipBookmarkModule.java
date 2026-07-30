package dev.valarisclient.core.modules.creator;

import dev.valarisclient.core.clip.ClipRecorder;
import dev.valarisclient.core.keybind.Keybind;
import dev.valarisclient.core.keybind.KeybindManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.notification.NotificationManager;
import dev.valarisclient.core.replay.ReplaySession;

/** Keybind to bookmark timestamp in clip/replay session. */
public final class ClipBookmarkModule extends Module {

    private static final int DEFAULT_KEY = 298; // F9

    private final ClipRecorder clipRecorder;
    private final ReplaySession replaySession;
    private final NotificationManager notifications;
    private int bookmarkCount;

    public ClipBookmarkModule(ClipRecorder clipRecorder, ReplaySession replaySession,
                              KeybindManager keybinds, NotificationManager notifications) {
        super("clip-bookmark", "Clip Bookmark", "Bookmark moments during recording", ModuleCategory.CREATOR);
        this.clipRecorder = clipRecorder;
        this.replaySession = replaySession;
        this.notifications = notifications;

        keybinds.register(new Keybind("clip-bookmark", "Clip Bookmark", "Creator", DEFAULT_KEY)
                .onPress(this::bookmark));
    }

    @Override
    protected void onEnable() {
        bookmarkCount = 0;
    }

    private void bookmark() {
        if (!isEnabled()) {
            return;
        }
        bookmarkCount++;
        String context;
        if (clipRecorder.isRecording()) {
            context = "clip frame " + clipRecorder.capturedFrameCount();
        } else if (replaySession.recording()) {
            context = "replay frame " + replaySession.frames().size();
        } else {
            context = "timestamp";
        }
        notifications.info("Bookmark #" + bookmarkCount, context);
    }
}
