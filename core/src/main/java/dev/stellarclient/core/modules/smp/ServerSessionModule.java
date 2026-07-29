package dev.stellarclient.core.modules.smp;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.hud.HudAnchor;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.theme.ThemeManager;

/** Shows current server and session playtime. */
public final class ServerSessionModule extends Module {

    private final SmpLineHud element;
    private final MinecraftAdapter adapter;

    public ServerSessionModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("server-session", "Server Session", "Server name and time played this session", ModuleCategory.SURVIVAL);
        this.adapter = adapter;
        this.element = hud.register(new SmpLineHud(
                "server-session", "Server Session", themes, HudAnchor.TOP_CENTER, 0, 68));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> refresh());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
        refresh();
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
    }

    private void refresh() {
        long millis = adapter.sessionMillis();
        int min = (int) (millis / 60_000);
        int sec = (int) ((millis / 1000) % 60);
        String server = adapter.serverAddress();
        if (server.isBlank()) {
            server = "Offline";
        }
        if (server.length() > 18) {
            server = server.substring(0, 15) + "...";
        }
        element.setText(server + " | " + min + ":" + String.format("%02d", sec));
    }
}
