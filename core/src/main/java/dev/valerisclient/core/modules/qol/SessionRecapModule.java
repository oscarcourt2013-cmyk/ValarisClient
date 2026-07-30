package dev.valerisclient.core.modules.qol;

import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.event.PlayerDeathEvent;
import dev.valerisclient.core.event.WorldLeaveEvent;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.notification.NotificationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Session recap on disconnect or world leave. */
public final class SessionRecapModule extends Module {


    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;

    private int deaths;
    private int pearlsThrown;
    private long sessionStart;

    public SessionRecapModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("session-recap", "Session Recap", "Summary when leaving a world", ModuleCategory.QOL);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(PlayerDeathEvent.class, event -> deaths++);
        listen(WorldLeaveEvent.class, event -> showRecap());
    }

    @Override
    protected void onEnable() {
        deaths = 0;
        pearlsThrown = 0;
        sessionStart = System.currentTimeMillis();
    }

    /** Called from pearl cooldown tracking via adapter heuristic on disable. */
    public void trackPearlThrow() {
        pearlsThrown++;
    }

    private void showRecap() {
        long minutes = Math.max(1, (System.currentTimeMillis() - sessionStart) / 60_000L);
        notifications.info("Session Recap",
                minutes + " min · " + deaths + " deaths · ~" + pearlsThrown + " pearls");
    }

}
