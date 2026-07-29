package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

/** Warns when health drops below a threshold mid-fight. */
public final class HealthAlertModule extends Module {

    private final IntSetting threshold = addSetting(new IntSetting(
            "threshold", "Threshold HP", "Warn when health is at or below this", 6, 1, 19));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private boolean warned;

    public HealthAlertModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("health-alert", "Health Alert", "Notification when HP is critically low", ModuleCategory.PVP);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> onTick());
    }

    @Override
    protected void onEnable() {
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
        float hp = adapter.playerHealth();
        if (hp > threshold.get()) {
            warned = false;
            return;
        }
        if (!warned) {
            warned = true;
            notifications.warning("Low HP", "Health at " + formatHealth(hp) + " â€” heal or pop!");
        }
    }

    private static String formatHealth(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format("%.1f", value);
    }
}
