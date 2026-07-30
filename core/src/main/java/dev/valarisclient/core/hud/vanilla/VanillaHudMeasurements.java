package dev.valarisclient.core.hud.vanilla;

/** Last measured on-screen bounds for vanilla HUD widgets with variable size. */
public final class VanillaHudMeasurements {

    public record Bounds(int x, int y, int width, int height) {
        public static final Bounds INVALID = new Bounds(0, 0, 0, 0);

        public boolean valid() {
            return width > 0 && height > 0;
        }
    }

    private static Bounds scoreboard = Bounds.INVALID;
    private static boolean capturingScoreboard;
    private static boolean capturedAnyFill;
    private static boolean captureCommitted;
    private static int captureMinX = Integer.MAX_VALUE;
    private static int captureMinY = Integer.MAX_VALUE;
    private static int captureMaxX = Integer.MIN_VALUE;
    private static int captureMaxY = Integer.MIN_VALUE;

    private VanillaHudMeasurements() {
    }

    public static void setScoreboard(Bounds bounds) {
        scoreboard = bounds != null ? bounds : Bounds.INVALID;
    }

    public static void clearScoreboard() {
        scoreboard = Bounds.INVALID;
    }

    public static Bounds scoreboard() {
        return scoreboard;
    }

    public static void beginScoreboardCapture() {
        capturingScoreboard = true;
        capturedAnyFill = false;
        captureMinX = Integer.MAX_VALUE;
        captureMinY = Integer.MAX_VALUE;
        captureMaxX = Integer.MIN_VALUE;
        captureMaxY = Integer.MIN_VALUE;
    }

    public static void recordScoreboardFill(int x1, int y1, int x2, int y2) {
        if (!capturingScoreboard) {
            return;
        }
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        if (maxX <= minX || maxY <= minY) {
            return;
        }
        capturedAnyFill = true;
        captureMinX = Math.min(captureMinX, minX);
        captureMinY = Math.min(captureMinY, minY);
        captureMaxX = Math.max(captureMaxX, maxX);
        captureMaxY = Math.max(captureMaxY, maxY);
    }

    public static void endScoreboardCapture() {
        if (!capturingScoreboard) {
            return;
        }
        capturingScoreboard = false;
        if (capturedAnyFill) {
            setScoreboard(new Bounds(captureMinX, captureMinY, captureMaxX - captureMinX, captureMaxY - captureMinY));
            captureCommitted = true;
        }
    }

    /** True if a capture committed bounds since the last call; callers fall back to computed metrics otherwise. */
    public static boolean consumeScoreboardCaptureCommitted() {
        boolean committed = captureCommitted;
        captureCommitted = false;
        return committed;
    }
}
