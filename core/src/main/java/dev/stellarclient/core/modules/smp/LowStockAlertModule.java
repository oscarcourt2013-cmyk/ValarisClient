package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.notification.NotificationManager;

/** Warns when held item stack falls below a minimum count. */
public final class LowStockAlertModule extends Module {

    private final IntSetting minCount = addSetting(new IntSetting(
            "min-count", "Min count", "Alert when stack count is at or below this", 16, 1, 64));
    private final StringSetting itemFilter = addSetting(new StringSetting(
            "item-filter", "Item filter", "Optional item name filter (empty = any item)", ""));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private boolean warned;

    public LowStockAlertModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("low-stock-alert", "Low Stock Alert", "Warns when a resource stack is running low", ModuleCategory.QOL);
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
        String name = adapter.heldItemName();
        int count = adapter.heldItemCount();
        if (name.isEmpty()) {
            warned = false;
            return;
        }
        String filter = itemFilter.get().trim().toLowerCase();
        if (!filter.isEmpty() && !name.toLowerCase().contains(filter)) {
            warned = false;
            return;
        }
        if (count > minCount.get()) {
            warned = false;
            return;
        }
        if (!warned) {
            warned = true;
            notifications.warning("Low Stock", name + " x" + count + " â€” restock soon");
        }
    }
}
