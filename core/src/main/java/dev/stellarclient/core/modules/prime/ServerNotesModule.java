package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.StringSetting;
import dev.stellarclient.core.notification.NotificationManager;

/** Per-server notes stored in module settings. */
public final class ServerNotesModule extends Module {

    private final StringSetting notes =
            addSetting(new StringSetting("notes", "Notes", "Notes for current server", ""));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private String lastServer = "";

    public ServerNotesModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("server-notes", "Server Notes", "Per-server notes in settings", ModuleCategory.PRIME);
        this.adapter = adapter;
        this.notifications = notifications;
        listen(ClientTickEvent.class, event -> checkServer());
    }

    @Override
    protected void onEnable() {
        showNotes();
    }

    private void checkServer() {
        String server = adapter.serverAddress();
        if (server.equals(lastServer)) {
            return;
        }
        lastServer = server;
        showNotes();
    }

    private void showNotes() {
        String note = notes.get().trim();
        if (!note.isEmpty()) {
            notifications.info("Server Notes", adapter.serverAddress() + ": " + note);
        }
    }
}
