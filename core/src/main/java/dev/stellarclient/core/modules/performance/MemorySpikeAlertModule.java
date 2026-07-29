package dev.stellarclient.core.modules.performance;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

/** Warns on sudden JVM heap spikes. */
public final class MemorySpikeAlertModule extends Module {

    private final IntSetting thresholdMb =
            addSetting(new IntSetting("threshold", "Threshold MB", "Spike size to alert", 256, 64, 1024));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;

    private long lastUsed = -1;
    private long cooldownUntil;

    public MemorySpikeAlertModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("memory-spike-alert", "Memory Spike Alert", "Warn on heap usage spikes", ModuleCategory.PERFORMANCE);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> check());
    }

    @Override
    protected void onDisable() {
        lastUsed = -1;
    }

    private void check() {
        long used = adapter.usedMemoryMb();
        if (lastUsed < 0) {
            lastUsed = used;
            return;
        }
        long delta = used - lastUsed;
        lastUsed = used;
        if (delta >= thresholdMb.get() && System.currentTimeMillis() > cooldownUntil) {
            notifications.warning("Memory Spike", "+" + delta + " MB (now " + used + " MB)");
            cooldownUntil = System.currentTimeMillis() + 30_000L;
        }
    }
}
