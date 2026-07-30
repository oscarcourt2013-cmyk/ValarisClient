package dev.stellarclient.core.modules.prime;

import dev.stellarclient.core.account.PrimeAccountService;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.discord.DiscordRpcService;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.event.WorldJoinEvent;
import dev.stellarclient.core.event.WorldLeaveEvent;
import dev.stellarclient.core.module.BooleanSetting;
import dev.stellarclient.core.module.IntSetting;
import dev.stellarclient.core.module.Module;
import dev.stellarclient.core.module.ModuleCategory;
import dev.stellarclient.core.module.ModuleManager;

/**
 * Discord Rich Presence — clean server context, compact stats, Prime branding.
 *
 * <p>Application ID: {@link DiscordRpcService#APPLICATION_ID}</p>
 */
public final class DiscordRichPresenceModule extends Module {

    private final BooleanSetting showServerIp =
            addSetting(new BooleanSetting("server-ip", "Show server", "Show server name / address in presence", true));
    private final BooleanSetting showHealth =
            addSetting(new BooleanSetting("health", "Show health", "HP on the secondary line", true));
    private final BooleanSetting showPing =
            addSetting(new BooleanSetting("ping", "Show ping", "Latency on the secondary line", true));
    private final BooleanSetting showBiome =
            addSetting(new BooleanSetting("biome", "Show biome", "Include current biome (can clutter)", false));
    private final BooleanSetting showCoordinates =
            addSetting(new BooleanSetting("coords", "Show coordinates", "Include XYZ (can clutter)", false));
    private final BooleanSetting showHeldItem =
            addSetting(new BooleanSetting("held-item", "Show held item", "Include item in hand (can clutter)", false));
    private final BooleanSetting showModuleCount =
            addSetting(new BooleanSetting("modules", "Show modules", "Short enabled-module count", false));
    private final BooleanSetting showSessionTime =
            addSetting(new BooleanSetting("session", "Session timer", "Show elapsed session time", true));
    private final BooleanSetting showAccountTier =
            addSetting(new BooleanSetting("tier", "Show Prime tier", "Include account tier on secondary line", false));
    private final BooleanSetting showFps =
            addSetting(new BooleanSetting("fps", "Show FPS", "Include current FPS", false));
    private final IntSetting updateInterval =
            addSetting(new IntSetting("interval", "Update interval", "Ticks between RPC refresh", 40, 20, 200));

    private final DiscordRpcService discord;
    private final MinecraftAdapter adapter;
    private final ModuleManager modules;
    private final PrimeAccountService account;

    public DiscordRichPresenceModule(DiscordRpcService discord, MinecraftAdapter adapter,
                                     ModuleManager modules, PrimeAccountService account) {
        super("discord-rpc", "Discord RPC", "Clean Rich Presence with server context and compact stats",
                ModuleCategory.PRIME);
        this.discord = discord;
        this.adapter = adapter;
        this.modules = modules;
        this.account = account;

        listen(ClientTickEvent.class, event -> onTick());
        listen(WorldJoinEvent.class, event -> {
            syncSettings();
            discord.onWorldJoin();
            discord.forceUpdate(adapter, modules, account);
        });
        listen(WorldLeaveEvent.class, event -> discord.onWorldLeave());
    }

    @Override
    protected void onEnable() {
        syncSettings();
        discord.start();
        discord.forceUpdate(adapter, modules, account);
    }

    @Override
    protected void onDisable() {
        discord.stop();
    }

    private void onTick() {
        syncSettings();
        discord.tick(adapter, modules, account);
    }

    private void syncSettings() {
        var s = discord.settings();
        s.setShowServerIp(showServerIp.get());
        s.setShowHealth(showHealth.get());
        s.setShowPing(showPing.get());
        s.setShowBiome(showBiome.get());
        s.setShowCoordinates(showCoordinates.get());
        s.setShowHeldItem(showHeldItem.get());
        s.setShowModuleCount(showModuleCount.get());
        s.setShowSessionTime(showSessionTime.get());
        s.setShowAccountTier(showAccountTier.get());
        s.setShowFps(showFps.get());
        s.setUpdateIntervalTicks(updateInterval.get());
    }
}
