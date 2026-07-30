package dev.valerisclient.core.modules.pvp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.IntSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.notification.NotificationManager;

/** Warns when totem count drops below minimum for crystal PvP. */
public final class TotemAlertModule extends Module {

    private final IntSetting minTotems = addSetting(new IntSetting(
            "min-totems", "Min totems", "Warn when totem count is at or below this", 1, 0, 64));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private boolean warned;

    public TotemAlertModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("totem-alert", "Totem Alert", "Warns when you are running out of totems", ModuleCategory.PVP);
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
        int count = adapter.countItemsMatching("totem");
        if (count > minTotems.get()) {
            warned = false;
            return;
        }
        if (!warned) {
            warned = true;
            notifications.warning("Totem Alert", "Only " + count + " totem(s) left!");
        }
    }
}
