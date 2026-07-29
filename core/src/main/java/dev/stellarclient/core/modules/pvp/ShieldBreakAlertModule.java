package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

/** Notifies when the target's shield durability is low. */
public final class ShieldBreakAlertModule extends Module {

    private final IntSetting threshold =
            addSetting(new IntSetting("threshold", "Threshold", "Alert below this percent", 25, 5, 50));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private boolean alerted;

    public ShieldBreakAlertModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("shield-break-alert", "Shield Break Alert", "Warn when target shield is low", ModuleCategory.PVP);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> check());
    }

    @Override
    protected void onDisable() {
        alerted = false;
    }

    private void check() {
        if (!adapter.hasTarget() || !adapter.targetBlocking()) {
            alerted = false;
            return;
        }
        int percent = adapter.targetShieldDurabilityPercent();
        if (percent < 0) {
            return;
        }
        if (percent <= threshold.get() && !alerted) {
            notifications.warning("Shield Break", adapter.targetName() + " shield at " + percent + "%");
            alerted = true;
        } else if (percent > threshold.get()) {
            alerted = false;
        }
    }
}
