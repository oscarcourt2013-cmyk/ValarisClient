package dev.valerisclient.core.modules.creator;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.adapter.RenderContext;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.hud.HudAnchor;
import dev.valerisclient.core.hud.HudElement;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.replay.ReplayFrame;
import dev.valerisclient.core.replay.ReplaySession;
import dev.valerisclient.core.replay.ReplayStorage;
import dev.valerisclient.core.theme.Theme;
import dev.valerisclient.core.theme.ThemeManager;
import dev.valerisclient.core.util.ColorUtil;

import java.util.List;

/** Lightweight replay recorder with trail, ghost marker and playback controls. */
public final class ReplayToolsModule extends Module {

    private final BooleanSetting autoRecord =
            addSetting(new BooleanSetting("record", "Auto record", "Record while enabled", true));
    private final BooleanSetting showTrail =
            addSetting(new BooleanSetting("trail", "Show trail", "Draw movement trail overlay", true));
    private final BooleanSetting showGhost =
            addSetting(new BooleanSetting("ghost", "Ghost player", "Show playback ghost marker", true));
    private final StringSetting replayFile =
            addSetting(new StringSetting("file", "Replay file", "Name for save/load", "latest"));
    private final BooleanSetting saveReplay =
            addSetting(new BooleanSetting("save", "Save replay", "Write timeline to disk", false));
    private final BooleanSetting loadReplay =
            addSetting(new BooleanSetting("load", "Load replay", "Load timeline from disk", false));

    private final MinecraftAdapter adapter;
    private final ReplaySession session;
    private final ReplayStorage storage;
    private final HudElement overlay;

    public ReplayToolsModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter,
                             ReplaySession session, ReplayStorage storage) {
        super("replay-tools", "Replay Tools", "Record and replay movement", ModuleCategory.CREATOR);
        this.adapter = adapter;
        this.session = session;
        this.storage = storage;
        this.overlay = hud.register(new OverlayElement(themes, session, showTrail, showGhost));
        overlay.setVisible(false);
        listen(ClientTickEvent.class, event -> tick());
    }

    @Override
    protected void onEnable() {
        overlay.setVisible(true);
        if (autoRecord.get()) {
            session.startRecording();
        }
    }

    @Override
    protected void onDisable() {
        session.stopRecording();
        session.stopPlayback();
        overlay.setVisible(false);
    }

    public void togglePlayback() {
        if (session.playing()) {
            session.togglePause();
        } else {
            session.startPlayback();
        }
    }

    public void cycleSpeed() {
        session.cycleSpeed();
    }

    private void tick() {
        overlay.setVisible(isEnabled());
        if (saveReplay.get()) {
            storage.save(session, replayFile.get());
            saveReplay.set(false);
        }
        if (loadReplay.get()) {
            if (storage.load(session, replayFile.get())) {
                session.stopRecording();
            }
            loadReplay.set(false);
        }
        if (!isEnabled() || !adapter.hasPlayer()) {
            return;
        }
        if (autoRecord.get() && !session.playing()) {
            if (!session.recording()) {
                session.startRecording();
            }
            session.recordFrame(new ReplayFrame(
                    System.currentTimeMillis(),
                    adapter.playerX(), adapter.playerY(), adapter.playerZ(),
                    adapter.playerYaw(), adapter.playerPitch(),
                    adapter.isSprinting(), adapter.isSneaking()));
        }
        session.tickPlayback(1f / 20f);
    }

    private static final class OverlayElement extends HudElement {
        private static final int PADDING = 4;

        private final ThemeManager themes;
        private final ReplaySession session;
        private final BooleanSetting showTrail;
        private final BooleanSetting showGhost;

        OverlayElement(ThemeManager themes, ReplaySession session,
                       BooleanSetting showTrail, BooleanSetting showGhost) {
            super("replay-overlay", "Replay Overlay", HudAnchor.BOTTOM_LEFT, 4, -4);
            this.themes = themes;
            this.session = session;
            this.showTrail = showTrail;
            this.showGhost = showGhost;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return 140;
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return 56;
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            Theme theme = themes.active();
            int w = measureWidth(ctx);
            int h = measureHeight(ctx);
            ctx.fillRect(0, 0, w, h, theme.background());
            ctx.fillRect(0, 0, w, 1, theme.accent());

            String status = session.recording() ? "REC" : session.playing() ? (session.paused() ? "PAUSED" : "PLAY") : "IDLE";
            ctx.drawText(status + "  " + session.frames().size() + " pts  " + session.speed() + "x",
                    PADDING, PADDING, theme.foreground(), true);

            if (showTrail.get()) {
                drawTrail(ctx, theme);
            }
            if (showGhost.get()) {
                drawGhost(ctx, theme);
            }
        }

        private void drawTrail(RenderContext ctx, Theme theme) {
            List<ReplayFrame> frames = session.frames();
            if (frames.size() < 2 || !adapterHasPlayer()) {
                return;
            }
            ReplayFrame latest = frames.getLast();
            double baseX = latest.x();
            double baseZ = latest.z();
            int ox = PADDING;
            int oy = 28;
            ReplayFrame prev = null;
            int drawn = 0;
            for (int i = frames.size() - 1; i >= 0 && drawn < 24; i--) {
                ReplayFrame f = frames.get(i);
                if (prev != null) {
                    int x1 = ox + (int) ((prev.x() - baseX) * 4);
                    int y1 = oy + (int) ((prev.z() - baseZ) * 4);
                    int x2 = ox + (int) ((f.x() - baseX) * 4);
                    int y2 = oy + (int) ((f.z() - baseZ) * 4);
                    drawLine(ctx, x1, y1, x2, y2, ColorUtil.withAlpha(theme.accent(), 0.35f));
                }
                prev = f;
                drawn++;
            }
        }

        private void drawGhost(RenderContext ctx, Theme theme) {
            ReplayFrame ghost = session.ghostFrame();
            if (ghost == null) {
                return;
            }
            ctx.fillRect(100, 30, 6, 6, ColorUtil.withAlpha(theme.accentSecondary(), 0.8f));
            ctx.drawText("Ghost", 108, 30, theme.foregroundMuted(), true);
        }

        private static void drawLine(RenderContext ctx, int x1, int y1, int x2, int y2, int color) {
            int dx = Math.abs(x2 - x1);
            int dy = Math.abs(y2 - y1);
            int sx = x1 < x2 ? 1 : -1;
            int sy = y1 < y2 ? 1 : -1;
            int err = dx - dy;
            while (true) {
                ctx.fillRect(x1, y1, 1, 1, color);
                if (x1 == x2 && y1 == y2) break;
                int e2 = err * 2;
                if (e2 > -dy) { err -= dy; x1 += sx; }
                if (e2 < dx) { err += dx; y1 += sy; }
            }
        }

        private boolean adapterHasPlayer() {
            return !session.frames().isEmpty();
        }
    }
}
