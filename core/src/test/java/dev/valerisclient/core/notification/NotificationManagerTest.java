package dev.valerisclient.core.notification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationManagerTest {

    @Test
    void oldestIsEvictedBeyondCapacity() {
        NotificationManager manager = new NotificationManager();
        for (int i = 0; i < 7; i++) {
            manager.info("title-" + i, "message");
        }
        assertEquals(5, manager.activeCount());

        List<String> titles = new ArrayList<>();
        manager.forEachActive(System.currentTimeMillis(), n -> titles.add(n.title()));
        assertEquals(List.of("title-2", "title-3", "title-4", "title-5", "title-6"), titles);
    }

    @Test
    void expiredNotificationsArePruned() {
        NotificationManager manager = new NotificationManager();
        long now = System.currentTimeMillis();
        manager.push(new Notification("old", "m", Notification.Level.INFO, now - 10_000, 4000));
        manager.push(new Notification("fresh", "m", Notification.Level.INFO, now, 4000));

        List<String> titles = new ArrayList<>();
        manager.forEachActive(now, n -> titles.add(n.title()));
        assertEquals(List.of("fresh"), titles);
    }

    @Test
    void progressIsClamped() {
        long now = System.currentTimeMillis();
        Notification notification = new Notification("t", "m", Notification.Level.INFO, now, 1000);
        assertEquals(0f, notification.progress(now));
        assertEquals(1f, notification.progress(now + 5000));
    }
}
