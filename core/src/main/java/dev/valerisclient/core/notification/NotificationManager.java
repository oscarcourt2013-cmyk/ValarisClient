package dev.valerisclient.core.notification;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Queue of on-screen notifications.
 *
 * <p>No tick loop: expired entries are pruned lazily whenever the queue is
 * touched. The HUD iterates via {@link #forEachActive} — no list copies are
 * allocated on the render path.</p>
 */
public final class NotificationManager {

    /** Oldest entries are evicted beyond this, screen space is finite. */
    private static final int MAX_ACTIVE = 5;

    private final ArrayDeque<Notification> active = new ArrayDeque<>(MAX_ACTIVE);

    public void push(Notification notification) {
        prune(System.currentTimeMillis());
        while (active.size() >= MAX_ACTIVE) {
            active.pollFirst();
        }
        active.addLast(notification);
    }

    public void info(String title, String message) {
        push(Notification.of(title, message, Notification.Level.INFO));
    }

    public void success(String title, String message) {
        push(Notification.of(title, message, Notification.Level.SUCCESS));
    }

    public void warning(String title, String message) {
        push(Notification.of(title, message, Notification.Level.WARNING));
    }

    public void error(String title, String message) {
        push(Notification.of(title, message, Notification.Level.ERROR));
    }

    /**
     * Visits every live notification in insertion order, pruning expired ones.
     * Called by the HUD once per frame.
     */
    public void forEachActive(long nowMillis, Consumer<Notification> visitor) {
        Iterator<Notification> it = active.iterator();
        while (it.hasNext()) {
            Notification notification = it.next();
            if (notification.isExpired(nowMillis)) {
                it.remove();
            } else {
                visitor.accept(notification);
            }
        }
    }

    public int activeCount() {
        prune(System.currentTimeMillis());
        return active.size();
    }

    public void clear() {
        active.clear();
    }

    private void prune(long nowMillis) {
        Iterator<Notification> it = active.iterator();
        while (it.hasNext()) {
            if (it.next().isExpired(nowMillis)) {
                it.remove();
            }
        }
    }
}
