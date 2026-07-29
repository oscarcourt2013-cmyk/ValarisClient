package dev.stellarclient.core.modules.qol;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.event.PlayerDeathEvent;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

import java.util.ArrayDeque;
import java.util.Deque;

/** Saves last N seconds of position snapshots on death. */
public final class DeathReplayModule extends Module {

    private record Snapshot(double x, double y, double z, long millis) {
    }

    private final IntSetting seconds =
            addSetting(new IntSetting("seconds", "Buffer", "Seconds of position history", 5, 3, 15));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private final Deque<Snapshot> buffer = new ArrayDeque<>();
    private int tickCounter;

    public DeathReplayModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("death-replay", "Death Replay", "Review last death position trail", ModuleCategory.QOL);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> record());
        listen(PlayerDeathEvent.class, this::onDeath);
    }

    @Override
    protected void onDisable() {
        buffer.clear();
    }

    private void record() {
        if (!adapter.hasPlayer()) {
            return;
        }
        tickCounter++;
        if (tickCounter % 4 != 0) {
            return;
        }
        buffer.addLast(new Snapshot(adapter.playerX(), adapter.playerY(), adapter.playerZ(),
                System.currentTimeMillis()));
        int max = seconds.get() * 5;
        while (buffer.size() > max) {
            buffer.removeFirst();
        }
    }

    private void onDeath(PlayerDeathEvent event) {
        Snapshot first = buffer.peekFirst();
        Snapshot last = buffer.peekLast();
        String trail = first != null && last != null
                ? String.format("from (%.0f,%.0f,%.0f) â†’ (%.0f,%.0f,%.0f)",
                first.x, first.y, first.z, last.x, last.y, last.z)
                : String.format("at (%.0f, %.0f, %.0f)", event.x(), event.y(), event.z());
        notifications.info("Death Replay", trail + " Â· " + buffer.size() + " snapshots");
        buffer.clear();
    }
}
