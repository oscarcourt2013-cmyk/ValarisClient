package dev.valerisclient.core.modules.valeris;

import dev.valerisclient.core.account.ValerisAccountService;
import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.discord.DiscordRpcService;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.event.WorldJoinEvent;
import dev.valerisclient.core.event.WorldLeaveEvent;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.IntSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;
import dev.valerisclient.core.module.ModuleManager;

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
            addSetting(new BooleanSetting("tier", "Show Valeris tier", "Include account tier on secondary line", false));
    private final BooleanSetting showFps =
            addSetting(new BooleanSetting("fps", "Show FPS", "Include current FPS", false));
    private final IntSetting updateInterval =
            addSetting(new IntSetting("interval", "Update interval", "Ticks between RPC refresh", 40, 20, 200));

    private final DiscordRpcService discord;
    private final MinecraftAdapter adapter;
    private final ModuleManager modules;
    private final ValerisAccountService account;

    public DiscordRichPresenceModule(DiscordRpcService discord, MinecraftAdapter adapter,
                                     ModuleManager modules, ValerisAccountService account) {
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
