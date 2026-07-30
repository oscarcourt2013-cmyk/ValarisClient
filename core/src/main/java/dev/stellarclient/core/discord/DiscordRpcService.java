package dev.stellarclient.core.discord;

import dev.stellarclient.core.account.PrimeAccountService;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.i18n.PrimeLang;
import dev.stellarclient.core.discord.ipc.DiscordIpcClient;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.servers.PartnerServers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Builds and publishes Discord Rich Presence from live game state. */
public final class DiscordRpcService {

    public static final String APPLICATION_ID = "1525574680994648174";

    private static final String WEBSITE_URL = "https://oscarcourt2013-cmyk.github.io/StellarClient/";
    private static final String DOWNLOAD_URL = "https://github.com/oscarcourt2013-cmyk/StellarClient/releases/latest";
    private static final String STATE_SEP = " · ";

    private final DiscordIpcClient ipc = new DiscordIpcClient(APPLICATION_ID);
    private final DiscordPresenceSettings settings = new DiscordPresenceSettings();

    private boolean running;
    private String lastFingerprint = "";
    private int tickCounter;
    private long menuSinceMillis = System.currentTimeMillis();
    private long worldSinceMillis;

    public DiscordPresenceSettings settings() {
        return settings;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        lastFingerprint = "";
        ipc.connectAsync();
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        lastFingerprint = "";
        ipc.clearAsync();
    }

    public void shutdown() {
        stop();
        ipc.shutdown();
    }

    public void onWorldJoin() {
        worldSinceMillis = System.currentTimeMillis();
        lastFingerprint = "";
    }

    public void onWorldLeave() {
        lastFingerprint = "";
        menuSinceMillis = System.currentTimeMillis();
    }

    public void tick(MinecraftAdapter adapter, ModuleManager modules, PrimeAccountService account) {
        if (!running) {
            return;
        }
        tickCounter++;
        if (tickCounter < settings.updateIntervalTicks()) {
            return;
        }
        tickCounter = 0;
        publishIfChanged(buildSnapshot(adapter, modules, account));
    }

    public void forceUpdate(MinecraftAdapter adapter, ModuleManager modules, PrimeAccountService account) {
        if (!running) {
            return;
        }
        if (adapter == null) {
            ipc.connectAsync();
            return;
        }
        publishIfChanged(buildSnapshot(adapter, modules, account));
    }

    private void publishIfChanged(DiscordPresenceSnapshot snapshot) {
        String fingerprint = snapshot.fingerprint();
        if (fingerprint.equals(lastFingerprint)) {
            return;
        }
        lastFingerprint = fingerprint;
        ipc.updateAsync(snapshot);
    }

    DiscordPresenceSnapshot buildSnapshot(MinecraftAdapter adapter, ModuleManager modules,
                                          PrimeAccountService account) {
        String mcVersion = adapter.minecraftVersion();
        String primeVersion = PrimeDesign.VERSION;

        if (!adapter.isInGame() || !adapter.hasPlayer()) {
            return menuPresence(mcVersion, primeVersion, adapter);
        }

        String server = adapter.serverAddress();
        boolean singleplayer = server == null || server.isBlank()
                || "Singleplayer".equalsIgnoreCase(server);

        String details = singleplayer
                ? PrimeLang.get("prime.discord.singleplayer", "Singleplayer")
                : serverLabel(server);

        String state = buildInGameState(adapter, modules, account, singleplayer);
        if (state.isEmpty()) {
            state = PrimeLang.get("prime.discord.playing_minecraft", "Playing Minecraft");
        }

        Long start = settings.showSessionTime()
                ? worldSinceMillis / 1000L
                : null;

        List<DiscordPresenceSnapshot.Button> buttons = buildButtons(server, singleplayer);

        return new DiscordPresenceSnapshot(
                details,
                state,
                "prime_logo",
                PrimeLang.get("prime.discord.client_version", "StellarClient v%s", primeVersion),
                "",
                "",
                start,
                buttons
        );
    }

    private String buildInGameState(MinecraftAdapter adapter, ModuleManager modules,
                                    PrimeAccountService account, boolean singleplayer) {
        List<String> parts = new ArrayList<>(4);

        if (settings.showHealth()) {
            parts.add("♥ " + Math.round(adapter.playerHealth())
                    + "/" + Math.round(adapter.playerMaxHealth()));
        }
        if (settings.showPing() && !singleplayer) {
            int ping = adapter.ping();
            if (ping > 0) {
                parts.add(ping + "ms");
            }
        }
        if (settings.showAccountTier() && account.loggedIn()) {
            parts.add(account.tier().name());
        }
        if (settings.showModuleCount()) {
            parts.add(PrimeLang.get("prime.discord.modules_short", "%d mods", countEnabled(modules)));
        }
        if (settings.showFps()) {
            parts.add(PrimeLang.get("prime.discord.fps", "%d FPS", adapter.fps()));
        }
        if (settings.showBiome()) {
            String biome = adapter.biomeName();
            if (!biome.isBlank()) {
                parts.add(biome);
            }
        }
        if (settings.showCoordinates()) {
            parts.add(formatCoord(adapter.playerX())
                    + ", " + formatCoord(adapter.playerY())
                    + ", " + formatCoord(adapter.playerZ()));
        }
        if (settings.showHeldItem()) {
            String item = adapter.heldItemName();
            if (!item.isBlank()) {
                parts.add(item);
            }
        }

        // Cap clutter: Discord state is one short line — keep at most 3 fragments.
        if (parts.size() > 3) {
            parts = parts.subList(0, 3);
        }
        return String.join(STATE_SEP, parts);
    }

    private DiscordPresenceSnapshot menuPresence(String mcVersion, String primeVersion,
                                                   MinecraftAdapter adapter) {
        String details = adapter.isScreenOpen()
                ? PrimeLang.get("prime.discord.browsing_menus", "Browsing menus")
                : PrimeLang.get("prime.discord.main_menu", "In Main Menu");
        String state = PrimeLang.get("prime.discord.state_menu", "Minecraft %1$s · Prime v%2$s",
                mcVersion, primeVersion);
        if (settings.showFps()) {
            state += STATE_SEP + PrimeLang.get("prime.discord.fps", "%d FPS", adapter.fps());
        }
        Long start = settings.showSessionTime() ? menuSinceMillis / 1000L : null;
        return new DiscordPresenceSnapshot(
                details,
                state,
                "prime_logo",
                PrimeLang.get("prime.discord.client_version", "StellarClient v%s", primeVersion),
                "",
                "",
                start,
                List.of(
                        new DiscordPresenceSnapshot.Button(
                                PrimeLang.get("prime.discord.button.website", "Website"), WEBSITE_URL),
                        new DiscordPresenceSnapshot.Button(
                                PrimeLang.get("prime.discord.button.download", "Download"), DOWNLOAD_URL)
                )
        );
    }

    private List<DiscordPresenceSnapshot.Button> buildButtons(String server, boolean singleplayer) {
        List<DiscordPresenceSnapshot.Button> buttons = new ArrayList<>(2);
        buttons.add(new DiscordPresenceSnapshot.Button(
                PrimeLang.get("prime.discord.button.website", "Website"), WEBSITE_URL));
        if (!singleplayer && settings.showServerIp() && server != null && !server.isBlank()) {
            buttons.add(new DiscordPresenceSnapshot.Button(
                    PrimeLang.get("prime.discord.button.server_status", "Server Status"),
                    serverStatusUrl(server)));
        } else {
            buttons.add(new DiscordPresenceSnapshot.Button(
                    PrimeLang.get("prime.discord.button.download", "Download"), DOWNLOAD_URL));
        }
        return buttons;
    }

    /** Prefer partner brand name, else clean hostname (no redundant "Playing on"). */
    private String serverLabel(String server) {
        if (!settings.showServerIp()) {
            return PrimeLang.get("prime.discord.multiplayer", "Multiplayer");
        }
        String partner = PartnerServers.partnerLabel(server);
        if (partner != null) {
            return partner;
        }
        return hostOnly(server);
    }

    private static int countEnabled(ModuleManager modules) {
        int count = 0;
        for (Module module : modules.all()) {
            if (module.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    private static String formatCoord(double value) {
        return Integer.toString((int) Math.floor(value));
    }

    private static String hostOnly(String server) {
        String s = server.trim();
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        int colon = s.indexOf(':');
        if (colon > 0) {
            // Keep IPv6-ish intact; typical host:port → host
            String after = s.substring(colon + 1);
            if (after.chars().allMatch(Character::isDigit)) {
                s = s.substring(0, colon);
            }
        }
        return s;
    }

    private static String serverStatusUrl(String server) {
        String encoded = URLEncoder.encode(server, StandardCharsets.UTF_8);
        return "https://mcsrvstat.us/server/" + encoded;
    }
}
