package dev.stellarclient.core.modules.survival;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.DoubleSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.notification.NotificationManager;

/** Alerts when leaving a waypoint-based base radius. */
public final class BaseRadiusModule extends Module {

    private final DoubleSetting centerX =
            addSetting(new DoubleSetting("center-x", "Center X", "Base center X", 0, -30_000_000, 30_000_000));
    private final DoubleSetting centerZ =
            addSetting(new DoubleSetting("center-z", "Center Z", "Base center Z", 0, -30_000_000, 30_000_000));
    private final DoubleSetting radius =
            addSetting(new DoubleSetting("radius", "Radius", "Base radius in blocks", 64, 8, 512));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private boolean wasInside = true;

    public BaseRadiusModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("base-radius", "Base Radius", "Alert when leaving your base area", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> check());
    }

    @Override
    protected void onEnable() {
        if (adapter.hasPlayer()) {
            centerX.set(adapter.playerX());
            centerZ.set(adapter.playerZ());
        }
        wasInside = true;
    }

    private void check() {
        if (!adapter.hasPlayer()) {
            return;
        }
        double dx = adapter.playerX() - centerX.get();
        double dz = adapter.playerZ() - centerZ.get();
        double dist = Math.sqrt(dx * dx + dz * dz);
        boolean inside = dist <= radius.get();
        if (wasInside && !inside) {
            notifications.warning("Base Radius", "Left base (" + Math.round(dist) + "m from center)");
        }
        wasInside = inside;
    }
}
