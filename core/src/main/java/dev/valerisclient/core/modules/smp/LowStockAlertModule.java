package dev.valerisclient.core.modules.smp;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.IntSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.StringSetting;
import dev.valerisclient.core.notification.NotificationManager;

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
            notifications.warning("Low Stock", name + " x" + count + " — restock soon");
        }
    }
}
