package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

/** Warns before AFK kick when no input is detected for too long. */
public final class AfkAlertModule extends Module {

    private final IntSetting idleMinutes = addSetting(new IntSetting(
            "idle-minutes", "Idle minutes", "Minutes without input before warning", 5, 1, 60));
    private final IntSetting warnMinutes = addSetting(new IntSetting(
            "warn-minutes", "Warn before", "Minutes before kick to warn", 1, 1, 10));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private long lastInputMillis = System.currentTimeMillis();
    private boolean warned;

    public AfkAlertModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("afk-alert", "AFK Alert", "Warns when you have been idle too long", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> onTick());
    }

    @Override
    protected void onEnable() {
        lastInputMillis = System.currentTimeMillis();
        warned = false;
    }

    @Override
    protected void onDisable() {
        warned = false;
    }

    private void onTick() {
        if (!adapter.isInGame() || !adapter.hasPlayer()) {
            return;
        }
        if (adapter.hasRecentInput() || adapter.isMouseButtonDown(0) || adapter.isMouseButtonDown(1)) {
            lastInputMillis = System.currentTimeMillis();
            warned = false;
            return;
        }
        long idleMillis = System.currentTimeMillis() - lastInputMillis;
        long warnThreshold = (idleMinutes.get() - warnMinutes.get()) * 60_000L;
        if (warnThreshold < 60_000L) {
            warnThreshold = 60_000L;
        }
        if (idleMillis >= warnThreshold && !warned) {
            warned = true;
            long remainingMin = Math.max(1, idleMinutes.get() - (idleMillis / 60_000L));
            notifications.warning("AFK Alert", "No input for a while â€” ~" + remainingMin + "m until kick?");
        }
    }
}
