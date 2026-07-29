package dev.stellarclient.core.modules.creator;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.clip.ClipRecorder;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudElement;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.keybind.Keybind;
import dev.stellarclient.core.keybind.KeybindManager;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.theme.ThemeManager;

/**
 * Records in-game video clips to {@code config/StellarClient/clips/*.mp4}.
 * StellarClient Launcher scans the same folder on the Media page.
 */
public final class ClipToolsModule extends Module {

    /** GLFW_KEY_F8 â€” rebindable via Prime keybind config. */
    private static final int DEFAULT_RECORD_KEY = 297;

    private final BooleanSetting recordToggle =
            addSetting(new BooleanSetting("record", "Record clip", "Toggle recording on/off", false));
    private final IntSetting fps =
            addSetting(new IntSetting("fps", "FPS", "Capture framerate", 30, 10, 60));
    private final IntSetting maxDuration =
            addSetting(new IntSetting("duration", "Max duration", "Seconds before auto-stop", 60, 5, 180));

    private final ClipRecorder recorder;
    private final MinecraftAdapter adapter;
    private final OverlayElement overlay;

    private boolean lastRecordToggle;

    public ClipToolsModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter,
                           ClipRecorder recorder, KeybindManager keybinds) {
        super("clip-recorder", "Clip Recorder", "Export MP4 clips to config/StellarClient/clips", ModuleCategory.CREATOR);
        this.recorder = recorder;
        this.adapter = adapter;
        this.overlay = hud.register(new OverlayElement(themes, recorder));
        overlay.setVisible(false);

        keybinds.register(new Keybind("clip-record", "Record Clip", "Creator", DEFAULT_RECORD_KEY)
                .onPress(this::onRecordHotkey));

        listen(ClientTickEvent.class, event -> onTick());
    }

    /** F8 (default): start/stop a clip up to the configured max duration. */
    private void onRecordHotkey() {
        if (!isEnabled() || adapter.isScreenOpen() || recorder.isEncoding()) {
            return;
        }
        recordToggle.set(!recordToggle.get());
    }

    @Override
    protected void onEnable() {
        recorder.configure(fps.get(), maxDuration.get());
        overlay.setVisible(true);
        lastRecordToggle = recordToggle.get();
    }

    @Override
    protected void onDisable() {
        if (recorder.isRecording()) {
            recorder.stop(adapter);
        }
        overlay.setVisible(false);
        recordToggle.set(false);
        lastRecordToggle = false;
    }

    private void onTick() {
        recorder.configure(fps.get(), maxDuration.get());

        boolean toggle = recordToggle.get();
        if (toggle != lastRecordToggle) {
            if (toggle) {
                recorder.start(adapter);
            } else if (recorder.isRecording()) {
                recorder.stop(adapter);
            }
            lastRecordToggle = toggle;
        }

        recorder.tick(adapter);
    }

    private static final class OverlayElement extends HudElement {
        private final ThemeManager themes;
        private final ClipRecorder recorder;

        OverlayElement(ThemeManager themes, ClipRecorder recorder) {
            super("clip-recorder-overlay", "Clip Recorder Overlay", HudAnchor.TOP_LEFT, 4, 4);
            this.themes = themes;
            this.recorder = recorder;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return ctx.textWidth(label()) + 6;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return ctx.fontHeight() + 6;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            if (!recorder.isRecording() && !recorder.isEncoding()) {
                return;
            }
            Theme theme = themes.active();
            String text = label();
            ctx.fillRect(0, 0, measureWidth(ctx), measureHeight(ctx), theme.background());
            ctx.drawText(text, 3, 3, recorder.isRecording() ? theme.error() : theme.accent(), true);
        }

        private String label() {
            if (recorder.isEncoding()) {
                return "CLIP ENCâ€¦";
            }
            return "REC " + formatTime(recorder.elapsedSeconds());
        }

        private static String formatTime(int totalSeconds) {
            int m = totalSeconds / 60;
            int s = totalSeconds % 60;
            return String.format("%d:%02d", m, s);
        }
    }
}
