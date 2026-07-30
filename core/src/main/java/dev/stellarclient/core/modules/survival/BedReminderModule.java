package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

/** Reminds you to sleep when night falls. */
public final class BedReminderModule extends Module {

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private boolean reminded;
    private boolean wasNight;

    public BedReminderModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("bed-reminder", "Bed Reminder", "Notification when night starts", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> onTick());
    }

    @Override
    protected void onEnable() {
        reminded = false;
        wasNight = SurvivalFormat.isNight(adapter.worldDayTime());
    }

    @Override
    protected void onDisable() {
        reminded = false;
    }

    private void onTick() {
        if (!adapter.isInGame()) {
            return;
        }
        boolean night = SurvivalFormat.isNight(adapter.worldDayTime());
        if (night && !wasNight && !reminded) {
            reminded = true;
            notifications.info("Bed Reminder", "Night is falling — consider sleeping");
        }
        if (!night) {
            reminded = false;
        }
        wasNight = night;
    }
}
