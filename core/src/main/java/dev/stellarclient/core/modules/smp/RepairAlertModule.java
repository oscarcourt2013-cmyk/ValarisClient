package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

/** Warns when armor durability drops below a threshold. */
public final class RepairAlertModule extends Module {

    private final IntSetting threshold = addSetting(new IntSetting(
            "threshold", "Threshold %", "Warn when any armor piece is below this durability", 25, 5, 80));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private boolean warned;

    public RepairAlertModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("repair-alert", "Repair Alert", "Warns when armor needs repair before dying", ModuleCategory.SURVIVAL);
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
        int lowest = 100;
        for (int slot = 0; slot < adapter.armorSlotCount(); slot++) {
            if (!adapter.hasArmor(slot)) {
                continue;
            }
            int max = adapter.armorMaxDurability(slot);
            if (max <= 0) {
                continue;
            }
            int percent = adapter.armorDurability(slot) * 100 / max;
            lowest = Math.min(lowest, percent);
        }
        if (lowest > threshold.get()) {
            warned = false;
            return;
        }
        if (!warned) {
            warned = true;
            notifications.warning("Repair Alert", "Armor at " + lowest + "% â€” repair soon");
        }
    }
}
