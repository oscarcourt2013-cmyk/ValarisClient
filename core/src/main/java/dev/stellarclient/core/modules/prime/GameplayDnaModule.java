package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.module.ModuleToggleEvent;
import dev.stellarclient.core.event.WorldLeaveEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.notification.NotificationManager;

import java.util.EnumMap;
import java.util.Map;

/** Tracks module usage patterns and suggests modules at session end. */
public final class GameplayDnaModule extends Module {

    private final ModuleManager modules;
    private final NotificationManager notifications;
    private final Map<ModuleCategory, Integer> toggles = new EnumMap<>(ModuleCategory.class);

    public GameplayDnaModule(ModuleManager modules, NotificationManager notifications) {
        super("gameplay-dna", "Gameplay DNA", "Learn your play style and suggest modules", ModuleCategory.PRIME);
        this.modules = modules;
        this.notifications = notifications;
        listen(ModuleToggleEvent.class, this::onToggle);
        listen(WorldLeaveEvent.class, event -> suggest());
    }

    @Override
    protected void onEnable() {
        toggles.clear();
    }

    private void onToggle(ModuleToggleEvent event) {
        if (!event.enabled()) {
            return;
        }
        toggles.merge(event.module().category(), 1, Integer::sum);
    }

    private void suggest() {
        ModuleCategory least = null;
        int min = Integer.MAX_VALUE;
        for (ModuleCategory cat : ModuleCategory.values()) {
            int count = toggles.getOrDefault(cat, 0);
            if (count < min) {
                min = count;
                least = cat;
            }
        }
        if (least == null) {
            return;
        }
        StringBuilder sb = new StringBuilder("Try: ");
        int added = 0;
        for (Module module : modules.all()) {
            if (module.category() == least && !module.isEnabled() && added < 3) {
                if (added > 0) {
                    sb.append(", ");
                }
                sb.append(module.name());
                added++;
            }
        }
        if (added > 0) {
            notifications.info("Gameplay DNA", sb.toString());
        }
    }
}
