package dev.valarisclient.core.modules.valaris;

import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.module.StringSetting;
import dev.valarisclient.core.notification.NotificationManager;

/** Per-server notes stored in module settings. */
public final class ServerNotesModule extends Module {

    private final StringSetting notes =
            addSetting(new StringSetting("notes", "Notes", "Notes for current server", ""));

    private final MinecraftAdapter adapter;
    private final NotificationManager notifications;
    private String lastServer = "";

    public ServerNotesModule(MinecraftAdapter adapter, NotificationManager notifications) {
        super("server-notes", "Server Notes", "Per-server notes in settings", ModuleCategory.VALARIS);
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
