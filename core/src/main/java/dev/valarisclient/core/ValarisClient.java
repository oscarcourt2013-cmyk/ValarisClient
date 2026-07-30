package dev.valarisclient.core;

import dev.valarisclient.core.account.ValarisAccountService;
import dev.valarisclient.core.adapter.MinecraftAdapter;
import dev.valarisclient.core.cloud.CloudSyncManager;
import dev.valarisclient.core.cloud.LocalCloudClient;
import dev.valarisclient.core.config.ConfigManager;
import dev.valarisclient.core.cosmetics.CosmeticManager;
import dev.valarisclient.core.crosshair.CrosshairConfig;
import dev.valarisclient.core.crosshair.CrosshairPresetStore;
import dev.valarisclient.core.crosshair.CrosshairProfileManager;
import dev.valarisclient.core.i18n.ValarisLang;
import dev.valarisclient.core.bootstrap.FirstRunConfigurator;
import dev.valarisclient.core.bootstrap.OnboardingFlow;
import dev.valarisclient.core.discord.DiscordRpcService;
import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.event.EventBus;
import dev.valarisclient.core.event.WorldJoinEvent;
import dev.valarisclient.core.event.WorldLeaveEvent;
import dev.valarisclient.core.module.ModuleToggleEvent;
import dev.valarisclient.core.gui.FavoritesManager;
import dev.valarisclient.core.gui.TooltipRenderer;
import dev.valarisclient.core.gui.clickgui.ClickGui;
import dev.valarisclient.core.gui.menu.LoadingOverlay;
import dev.valarisclient.core.gui.menu.OnboardingManager;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.hud.editor.HudEditor;
import dev.valarisclient.core.hud.elements.NotificationsElement;
import dev.valarisclient.core.hud.elements.WatermarkElement;
import dev.valarisclient.core.hud.vanilla.VanillaHudElements;
import dev.valarisclient.core.keybind.KeybindManager;
import dev.valarisclient.core.module.ModuleManager;
import dev.valarisclient.core.notification.NotificationManager;
import dev.valarisclient.core.notification.NotificationPreferences;
import dev.valarisclient.core.presence.ValarisPresenceService;
import dev.valarisclient.core.serverapi.ServerApiService;
import dev.valarisclient.core.skin.CustomSkinService;
import dev.valarisclient.core.profile.ProfileManager;
import dev.valarisclient.core.replay.ReplaySession;
import dev.valarisclient.core.replay.ReplayStorage;
import dev.valarisclient.core.clip.ClipRecorder;
import dev.valarisclient.core.clip.ClipStorage;
import dev.valarisclient.core.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** ValarisClient entry point and service root. */
public final class ValarisClient {

    /** Fabric mod id / Identifier namespace / config dir name. Must stay lowercase. */
    public static final String MOD_ID = "valarisclient";
    public static final String NAME = "ValarisClient";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static volatile ValarisClient instance;

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
    private final ValarisAccountService account;
    private final OnboardingManager onboarding;
    private final dev.valarisclient.core.servers.PartnerServerState partnerServers =
            new dev.valarisclient.core.servers.PartnerServerState();
    private final LoadingOverlay loadingOverlay;
    private final ReplaySession replaySession;
    private final ReplayStorage replayStorage;
    private final ClipStorage clipStorage;
    private final ClipRecorder clipRecorder;
    private final TooltipRenderer tooltips;
    private final DiscordRpcService discordRpc;
    private final ValarisPresenceService presence;
    private final ServerApiService serverApi;
    private final CustomSkinService customSkins;

    private boolean debutSession;
    private int debutTicks;
    private boolean debutMenuOpened;
    private boolean debutOverlayDone;

    private ValarisClient(MinecraftAdapter adapter) {
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
        this.account = new ValarisAccountService();
        this.discordRpc = new DiscordRpcService();
        this.presence = new ValarisPresenceService(adapter);
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
        configManager.register(partnerServers);
        configManager.register(notificationPrefs);
        configManager.register(discordRpc.settings());

        hud.register(new WatermarkElement(themes, adapter.minecraftVersion()));
        hud.register(new NotificationsElement(notifications, themes, notificationPrefs));
        VanillaHudElements.registerAll(hud);

        if (account.username().isBlank() && adapter.hasPlayer()) {
            account.login(adapter.playerName());
        }

        keybinds.register(new dev.valarisclient.core.keybind.Keybind("click-gui", "ClickGUI", "Valaris", 344)
                .onPress(adapter::openClickGui));
        keybinds.register(new dev.valarisclient.core.keybind.Keybind("hud-editor", "HUD Editor", "Valaris", 72)
                .onPress(adapter::openHudEditor));
    }

    public static synchronized void bootstrap(MinecraftAdapter adapter) {
        if (instance != null) {
            throw new IllegalStateException(NAME + " is already bootstrapped");
        }
        ValarisLang.bind(adapter::translate);
        ValarisClient client = new ValarisClient(adapter);
        instance = client;
        client.loadingOverlay.setStage(ValarisLang.get("valaris.gui.loading.core", "Loading Core..."), 0.2f);
        Modules.registerBuiltins(client);
        client.wireGlobalListeners();
        client.loadingOverlay.setStage(ValarisLang.get("valaris.gui.loading.modules", "Loading Modules..."), 0.55f);
        boolean freshInstall = client.profiles.loadInitial();
        client.debutSession = freshInstall;
        if (freshInstall) {
            FirstRunConfigurator.applyStarter(client);
        }
        var customSkin = client.modules.get("custom-skin");
        if (customSkin != null && !customSkin.isEnabled()) {
            customSkin.setEnabled(true);
        }
        client.loadingOverlay.setStage(ValarisLang.get("valaris.gui.loading.ready", "ValarisClient ready"), 1f);
        LOGGER.info("{} v{} bootstrapped (Minecraft {}, {} modules, profile '{}'{})",
                NAME, dev.valarisclient.core.design.ValarisDesign.VERSION,
                adapter.minecraftVersion(), client.modules.all().size(), client.profiles.activeProfile(),
                freshInstall ? ", first launch" : "");
    }

    private void wireGlobalListeners() {
        eventBus.subscribe(ModuleToggleEvent.class, event -> {
            if (!notificationPrefs.moduleToggleNotifs()) {
                return;
            }
            String verb = event.enabled()
                    ? ValarisLang.get("valaris.notification.module.enabled", "Enabled")
                    : ValarisLang.get("valaris.notification.module.disabled", "Disabled");
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

    public static ValarisClient get() {
        ValarisClient current = instance;
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
    public static ValarisClient getOrNull() {
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
    public ValarisAccountService account() { return account; }
    public OnboardingManager onboarding() { return onboarding; }
    public dev.valarisclient.core.servers.PartnerServerState partnerServers() { return partnerServers; }
    public LoadingOverlay loadingOverlay() { return loadingOverlay; }
    public ReplaySession replaySession() { return replaySession; }
    public ReplayStorage replayStorage() { return replayStorage; }
    public ClipStorage clipStorage() { return clipStorage; }
    public ClipRecorder clipRecorder() { return clipRecorder; }
    public TooltipRenderer tooltips() { return tooltips; }
    public DiscordRpcService discordRpc() { return discordRpc; }
    public ValarisPresenceService presence() { return presence; }
    public ServerApiService serverApi() { return serverApi; }
    public CustomSkinService customSkins() { return customSkins; }
}
