package dev.stellarclient.core.notification;

/**
 * One HUD notification. Immutable; expiry derives from creation time so no
 * per-tick bookkeeping is needed.
 */
public record Notification(
        String title,
        String message,
        Level level,
        long createdAtMillis,
        long durationMillis
) {

    public enum Level {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public static final long DEFAULT_DURATION_MILLIS = 4000;

    public static Notification of(String title, String message, Level level) {
        return new Notification(title, message, level, System.currentTimeMillis(), DEFAULT_DURATION_MILLIS);
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis - createdAtMillis >= durationMillis;
    }

    /** Progress of this notification's lifetime in [0, 1], for animations. */
    public float progress(long nowMillis) {
        return Math.clamp((nowMillis - createdAtMillis) / (float) durationMillis, 0f, 1f);
    }
}
