package dev.stellarclient.core.modules.pvp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

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
