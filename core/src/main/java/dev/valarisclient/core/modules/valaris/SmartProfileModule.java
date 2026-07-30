package dev.valarisclient.core.modules.valaris;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.BooleanSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.notification.NotificationManager;
import dev.valarisclient.core.profile.ProfileManager;

/** Auto-suggest profile from held item context. */
public final class SmartProfileModule extends Module {

    private final BooleanSetting autoApply =
            addSetting(new BooleanSetting("auto-apply", "Auto apply", "Switch profile hints automatically", false));

    private final MinecraftAdapter adapter;
    private final ProfileManager profiles;
    private final NotificationManager notifications;

    private String lastCategory = "";

    public SmartProfileModule(MinecraftAdapter adapter, ProfileManager profiles,
                              NotificationManager notifications) {
        super("smart-profile", "Smart Profile", "Suggest profile from held tool", ModuleCategory.VALARIS);
        this.adapter = adapter;
        this.profiles = profiles;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> evaluate());
    }

    private void evaluate() {
        if (!adapter.hasPlayer()) {
            return;
        }
        String category = adapter.heldToolCategory();
        if (category.equals(lastCategory)) {
            return;
        }
        lastCategory = category;
        if (category.isEmpty()) {
            return;
        }
        String preset = switch (category) {
            case "sword", "axe" -> "pvp";
            case "pickaxe", "shovel" -> "survival";
            default -> "";
        };
        if (preset.isEmpty()) {
            return;
        }
        notifications.info("Smart Profile", "Holding " + category + " → try " + preset.toUpperCase() + " profile");
        if (autoApply.get()) {
            profiles.switchTo(preset);
        }
    }
}
