package dev.valarisclient.core.modules.pvp;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.IntSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.notification.NotificationManager;

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
            notifications.warning("Low HP", "Health at " + formatHealth(hp) + " — heal or pop!");
        }
    }

    private static String formatHealth(float value) {
        return value == (int) value ? Integer.toString((int) value) : String.format("%.1f", value);
    }
}
