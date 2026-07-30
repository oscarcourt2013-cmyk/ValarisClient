package dev.valerisclient.core.modules.survival;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.notification.NotificationManager;

/** Bad omen level notification before raids. */
public final class RaidAlertModule extends Module {

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private int lastLevel = -1;

    public RaidAlertModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("raid-alert", "Raid Alert", "Warns when Bad Omen is active", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> check());
    }

    @Override
    protected void onDisable() {
        lastLevel = -1;
    }

    private void check() {
        int level = adapter.playerEffectAmplifier("bad_omen");
        if (level < 0) {
            lastLevel = -1;
            return;
        }
        if (level != lastLevel) {
            notifications.warning("Raid Alert", "Bad Omen level " + (level + 1));
            lastLevel = level;
        }
    }
}
