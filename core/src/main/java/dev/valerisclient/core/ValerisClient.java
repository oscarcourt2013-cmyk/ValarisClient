package dev.valerisclient.core;

import dev.valerisclient.core.account.PrimeAccountService;
import dev.valerisclient.core.adapter.MinecraftAdapter;
import dev.valerisclient.core.cloud.CloudSyncManager;
import dev.valerisclient.core.cloud.LocalCloudClient;
import dev.valerisclient.core.config.ConfigManager;
import dev.valerisclient.core.cosmetics.CosmeticManager;
import dev.valerisclient.core.crosshair.CrosshairConfig;
import dev.valerisclient.core.crosshair.CrosshairPresetStore;
import dev.valerisclient.core.crosshair.CrosshairProfileManager;
import dev.valerisclient.core.i18n.PrimeLang;
import dev.valerisclient.core.bootstrap.FirstRunConfigurator;
import dev.valerisclient.core.bootstrap.OnboardingFlow;
import dev.valerisclient.core.discord.DiscordRpcService;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.event.EventBus;
import dev.valerisclient.core.event.WorldJoinEvent;
import dev.valerisclient.core.event.WorldLeaveEvent;
import dev.valerisclient.core.module.ModuleToggleEvent;
import dev.valerisclient.core.gui.FavoritesManager;
import dev.valerisclient.core.gui.TooltipRenderer;
import dev.valerisclient.core.gui.clickgui.ClickGui;
import dev.valerisclient.core.gui.menu.LoadingOverlay;
import dev.valerisclient.core.gui.menu.OnboardingManager;
import dev.valerisclient.core.hud.HudManager;
import dev.valerisclient.core.hud.editor.HudEditor;
import dev.valerisclient.core.hud.elements.NotificationsElement;
import dev.valerisclient.core.hud.elements.WatermarkElement;
import dev.valerisclient.core.hud.vanilla.VanillaHudElements;
import dev.valerisclient.core.keybind.KeybindManager;
import dev.valerisclient.core.module.ModuleManager;
import dev.valerisclient.core.notification.NotificationManager;
import dev.valerisclient.core.notification.NotificationPreferences;
import dev.valerisclient.core.presence.PrimePresenceService;
import dev.valerisclient.core.serverapi.ServerApiService;
import dev.valerisclient.core.skin.CustomSkinService;
import dev.valerisclient.core.profile.ProfileManager;
import dev.valerisclient.core.replay.ReplaySession;
import dev.valerisclient.core.replay.ReplayStorage;
import dev.valerisclient.core.clip.ClipRecorder;
import dev.valerisclient.core.clip.ClipStorage;
import dev.valerisclient.core.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** ValerisClient entry point and service root. */
public final class ValerisClient {

    /** Fabric mod id / Identifier namespace / config dir name. Must stay lowercase. */
    public static final String MOD_ID = "valerisclient";
    public static final String NAME = "ValerisClient";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static volatile ValerisClient instance;

    private final MinecraftAdapter adapter;
    private final EventBus eventBus;
    private final ConfigManager configManager;
    private final KeybindManager keybinds;
    private final ModuleManager modules;
    private final ThemeManager themes;
    private final NotificationManager notifications;
    private final NotificationPreferences notificationPrefs;
    private final HudManager hud;
    private final HudEditor hudEditor;
    private final FavoritesManager favorites;
    private final ClickGui clickGui;
    private final ProfileManager profiles;
    private final CrosshairConfig crosshairConfig;
    private final CrosshairPresetStore crosshairPresets;
    private final CrosshairProfileManager crosshairProfiles;
    private final CosmeticManager cosmetics;
    private final CloudSyncManager cloudSync;
    private final PrimeAccountService account;
    private final OnboardingManager onboarding;
    private final LoadingOverlay loadingOverlay;
    private final ReplaySession replaySession;
    private final ReplayStorage replayStorage;
    private final ClipStorage clipStorage;
    private final ClipRecorder clipRecorder;
    private final TooltipRenderer tooltips;
    private final DiscordRpcService discordRpc;
    private final PrimePresenceService presence;
    private final ServerApiService serverApi;
    private final CustomSkinService customSkins;

    private boolean debutSession;
    private int debutTicks;
    private boolean debutMenuOpened;
    private boolean debutOverlayDone;

    private ValerisClient(MinecraftAdapter adapter) {
        this.adapter = adapter;
        this.eventBus = new EventBus();
        this.configManager = new ConfigManager();
        this.keybinds = new KeybindManager();
        this.modules = new ModuleManager(eventBus, keybinds);
        this.themes = new ThemeManager();
        this.notifications = new NotificationManager();
        this.notificationPrefs = new NotificationPreferences();
        this.hud = new HudManager();
        this.hudEditor = new HudEditor(hud, themes);
        this.favorites = new FavoritesManager();
        this.crosshairConfig = new CrosshairConfig();
        this.crosshairPresets = new CrosshairPresetStore();
        this.crosshairProfiles = new CrosshairProfileManager(crosshairConfig);
        this.cosmetics = new CosmeticManager();
        this.onboarding = new OnboardingManager();
        this.loadingOverlay = new LoadingOverlay();
        this.replaySession = new ReplaySession();
        this.tooltips = new TooltipRenderer();
        this.account = new PrimeAccountService();
        this.discordRpc = new DiscordRpcService();
        this.presence = new PrimePresenceService(adapter);
        this.serverApi = new ServerApiService(adapter, notifications);

        Path modRoot = adapter.configDirectory().resolve(MOD_ID);
        this.customSkins = new CustomSkinService(adapter, modRoot);
        LocalCloudClient localCloud = new LocalCloudClient(modRoot.resolve("cloud"));
        this.cloudSync = new CloudSyncManager(localCloud, configManager, notifications);
        this.replayStorage = new ReplayStorage(modRoot);
        this.clipStorage = new ClipStorage(modRoot);
        this.clipRecorder = new ClipRecorder(clipStorage, notifications);
        this.profiles = new ProfileManager(configManager, modRoot);
        this.hudEditor.setAutosaveHandler(profiles::saveActive);
        this.clickGui = new ClickGui(modules, themes, favorites, adapter, onboarding,
                cloudSync, cosmetics, profiles, keybinds, tooltips);
        this.clickGui.setOnboardingCompleteHandler(() -> OnboardingFlow.applyChoices(this));

        configManager.register(keybinds);
        configManager.register(modules);
        configManager.register(themes);
        configManager.register(hud);
        configManager.register(favorites);
        configManager.register(clickGui);
        configManager.register(crosshairConfig);
        configManager.register(crosshairPresets);
        configManager.register(crosshairProfiles);
        configManager.register(cosmetics);
        configManager.register(onboarding);
        configManager.register(account);
        configManager.register(notificationPrefs);
        configManager.register(discordRpc.settings());

        hud.register(new WatermarkElement(themes, adapter.minecraftVersion()));
        hud.register(new NotificationsElement(notifications, themes, notificationPrefs));
        VanillaHudElements.registerAll(hud);

        if (account.username().isBlank() && adapter.hasPlayer()) {
            account.login(adapter.playerName());
        }

        keybinds.register(new dev.valerisclient.core.keybind.Keybind("click-gui", "ClickGUI", "Valeris", 344)
                .onPress(adapter::openClickGui));
        keybinds.register(new dev.valerisclient.core.keybind.Keybind("hud-editor", "HUD Editor", "Valeris", 72)
                .onPress(adapter::openHudEditor));
    }

    public static synchronized void bootstrap(MinecraftAdapter adapter) {
        if (instance != null) {
            throw new IllegalStateException(NAME + " is already bootstrapped");
        }
        PrimeLang.bind(adapter::translate);
        ValerisClient client = new ValerisClient(adapter);
        instance = client;
        client.loadingOverlay.setStage(PrimeLang.get("prime.gui.loading.core", "Loading Core..."), 0.2f);
        Modules.registerBuiltins(client);
        client.wireGlobalListeners();
        client.loadingOverlay.setStage(PrimeLang.get("prime.gui.loading.modules", "Loading Modules..."), 0.55f);
        boolean freshInstall = client.profiles.loadInitial();
        client.debutSession = freshInstall;
        if (freshInstall) {
            FirstRunConfigurator.applyStarter(client);
        }
        var customSkin = client.modules.get("custom-skin");
        if (customSkin != null && !customSkin.isEnabled()) {
            customSkin.setEnabled(true);
        }
        client.loadingOverlay.setStage(PrimeLang.get("prime.gui.loading.ready", "ValerisClient ready"), 1f);
        LOGGER.info("{} v{} bootstrapped (Minecraft {}, {} modules, profile '{}'{})",
                NAME, dev.valerisclient.core.design.PrimeDesign.VERSION,
                adapter.minecraftVersion(), client.modules.all().size(), client.profiles.activeProfile(),
                freshInstall ? ", first launch" : "");
    }

    private void wireGlobalListeners() {
        eventBus.subscribe(ModuleToggleEvent.class, event -> {
            if (!notificationPrefs.moduleToggleNotifs()) {
                return;
            }
            String verb = event.enabled()
                    ? PrimeLang.get("prime.notification.module.enabled", "Enabled")
                    : PrimeLang.get("prime.notification.module.disabled", "Disabled");
            if (event.enabled()) {
                notifications.success(event.module().name(), verb);
            } else {
                notifications.info(event.module().name(), verb);
            }
        });
    }

    public void tick() {
        loadingOverlay.tick(1f / 20f);
        if (debutSession && loadingOverlay.finished() && !debutOverlayDone) {
            debutOverlayDone = true;
            debutTicks = 0;
        }
        if (debutSession && debutOverlayDone) {
            debutTicks++;
            if (debutTicks == 15 && !onboarding.completed() && !adapter.isScreenOpen()) {
                adapter.openClickGui();
                debutMenuOpened = true;
            }
            if (debutTicks > 200) {
                debutSession = false;
            }
        }
        tooltips.tick(50);
        profiles.pollExternalBridgeChanges();
        if (adapter.isScreenOpen()) {
            keybinds.releaseAll();
        } else {
            keybinds.poll(adapter::isKeyDown);
        }
        eventBus.post(ClientTickEvent.INSTANCE);
    }

    public void onWorldJoin() {
        loadingOverlay.requestDismiss();
        if (!account.loggedIn() && adapter.hasPlayer()) {
            account.login(adapter.playerName());
        }
        crosshairProfiles.applyForServer(adapter.serverAddress());
        presence.onWorldJoin();
        serverApi.onWorldJoin();
        eventBus.post(WorldJoinEvent.INSTANCE);
    }

    public void onWorldLeave() {
        crosshairProfiles.saveCurrentForServer(adapter.serverAddress());
        if (cloudSync.autoSync() && account.loggedIn()) {
            cloudSync.uploadNow(profiles.activeProfile());
        }
        presence.onWorldLeave();
        serverApi.onWorldLeave();
        eventBus.post(WorldLeaveEvent.INSTANCE);
    }

    public void shutdown() {
        discordRpc.shutdown();
        profiles.saveActive();
        LOGGER.info("{} shut down, config saved", NAME);
    }

    public static ValerisClient get() {
        ValerisClient current = instance;
        if (current == null) {
            throw new IllegalStateException(NAME + " is not bootstrapped yet");
        }
        return current;
    }

    /**
     * The client, or {@code null} when it has not bootstrapped yet.
     *
     * <p>For code that can run before the entrypoint completes -- vanilla screens
     * render during resource reload, so a throwing accessor there would take the
     * game down on startup.</p>
     */
    public static ValerisClient getOrNull() {
        return instance;
    }

    public MinecraftAdapter adapter() { return adapter; }
    public EventBus events() { return eventBus; }
    public ConfigManager config() { return configManager; }
    public KeybindManager keybinds() { return keybinds; }
    public ModuleManager modules() { return modules; }
    public ThemeManager themes() { return themes; }
    public NotificationManager notifications() { return notifications; }
    public NotificationPreferences notificationPrefs() { return notificationPrefs; }
    public HudManager hud() { return hud; }
    public HudEditor hudEditor() { return hudEditor; }
    public ClickGui clickGui() { return clickGui; }
    public FavoritesManager favorites() { return favorites; }
    public ProfileManager profiles() { return profiles; }
    public CrosshairConfig crosshairConfig() { return crosshairConfig; }
    public CrosshairPresetStore crosshairPresets() { return crosshairPresets; }
    public CrosshairProfileManager crosshairProfiles() { return crosshairProfiles; }
    public CosmeticManager cosmetics() { return cosmetics; }
    public CloudSyncManager cloudSync() { return cloudSync; }
    public PrimeAccountService account() { return account; }
    public OnboardingManager onboarding() { return onboarding; }
    public LoadingOverlay loadingOverlay() { return loadingOverlay; }
    public ReplaySession replaySession() { return replaySession; }
    public ReplayStorage replayStorage() { return replayStorage; }
    public ClipStorage clipStorage() { return clipStorage; }
    public ClipRecorder clipRecorder() { return clipRecorder; }
    public TooltipRenderer tooltips() { return tooltips; }
    public DiscordRpcService discordRpc() { return discordRpc; }
    public PrimePresenceService presence() { return presence; }
    public ServerApiService serverApi() { return serverApi; }
    public CustomSkinService customSkins() { return customSkins; }
}
