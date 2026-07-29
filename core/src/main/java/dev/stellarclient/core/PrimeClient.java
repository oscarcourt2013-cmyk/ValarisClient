package dev.stellarclient.core;

import dev.stellarclient.core.account.PrimeAccountService;
import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.ai.AiAssistantService;
import dev.stellarclient.core.cloud.CloudSyncManager;
import dev.stellarclient.core.cloud.LocalCloudClient;
import dev.stellarclient.core.config.ConfigManager;
import dev.stellarclient.core.cosmetics.CosmeticManager;
import dev.stellarclient.core.crosshair.CrosshairConfig;
import dev.stellarclient.core.crosshair.CrosshairPresetStore;
import dev.stellarclient.core.crosshair.CrosshairProfileManager;
import dev.stellarclient.core.i18n.PrimeLang;
import dev.stellarclient.core.bootstrap.FirstRunConfigurator;
import dev.stellarclient.core.bootstrap.OnboardingFlow;
import dev.stellarclient.core.discord.DiscordRpcService;
import dev.stellarclient.core.voice.VoiceChatService;
import dev.stellarclient.core.event.ClientTickEvent;
import dev.stellarclient.core.event.EventBus;
import dev.stellarclient.core.event.WorldJoinEvent;
import dev.stellarclient.core.event.WorldLeaveEvent;
import dev.stellarclient.core.module.ModuleToggleEvent;
import dev.stellarclient.core.gui.FavoritesManager;
import dev.stellarclient.core.gui.TooltipRenderer;
import dev.stellarclient.core.gui.clickgui.ClickGui;
import dev.stellarclient.core.gui.menu.LoadingOverlay;
import dev.stellarclient.core.gui.menu.OnboardingManager;
import dev.stellarclient.core.hud.HudManager;
import dev.stellarclient.core.hud.editor.HudEditor;
import dev.stellarclient.core.hud.elements.NotificationsElement;
import dev.stellarclient.core.hud.elements.WatermarkElement;
import dev.stellarclient.core.hud.vanilla.VanillaHudElements;
import dev.stellarclient.core.keybind.KeybindManager;
import dev.stellarclient.core.module.ModuleManager;
import dev.stellarclient.core.notification.NotificationManager;
import dev.stellarclient.core.notification.NotificationPreferences;
import dev.stellarclient.core.presence.PrimePresenceService;
import dev.stellarclient.core.serverapi.ServerApiService;
import dev.stellarclient.core.skin.CustomSkinService;
import dev.stellarclient.core.social.SocialService;
import dev.stellarclient.core.profile.ProfileManager;
import dev.stellarclient.core.replay.ReplaySession;
import dev.stellarclient.core.replay.ReplayStorage;
import dev.stellarclient.core.clip.ClipRecorder;
import dev.stellarclient.core.clip.ClipStorage;
import dev.stellarclient.core.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** StellarClient entry point and service root. */
public final class StellarClient {

    public static final String MOD_ID = "StellarClient";
    public static final String NAME = "StellarClient";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static volatile StellarClient instance;

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
    private final VoiceChatService voiceChat;
    private final PrimePresenceService presence;
    private final SocialService social;
    private final ServerApiService serverApi;
    private final AiAssistantService ai;
    private final CustomSkinService customSkins;

    private boolean debutSession;
    private int debutTicks;
    private boolean debutMenuOpened;
    private boolean debutOverlayDone;

    private StellarClient(MinecraftAdapter adapter) {
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
        this.voiceChat = new VoiceChatService();
        this.presence = new PrimePresenceService(adapter);
        this.social = new SocialService(adapter, notifications);
        this.serverApi = new ServerApiService(adapter, notifications, social);

        Path modRoot = adapter.configDirectory().resolve(MOD_ID);
        this.ai = new AiAssistantService(adapter, modules, modRoot, () -> social.settings().apiBase());
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
        configManager.register(voiceChat.settings());
        configManager.register(social.settings());

        hud.register(new WatermarkElement(themes, adapter.minecraftVersion()));
        hud.register(new NotificationsElement(notifications, themes, notificationPrefs));
        VanillaHudElements.registerAll(hud);

        if (account.username().isBlank() && adapter.hasPlayer()) {
            account.login(adapter.playerName());
        }

        keybinds.register(new dev.stellarclient.core.keybind.Keybind("click-gui", "ClickGUI", "Prime", 344)
                .onPress(adapter::openClickGui));
        keybinds.register(new dev.stellarclient.core.keybind.Keybind("hud-editor", "HUD Editor", "Prime", 72)
                .onPress(adapter::openHudEditor));
    }

    public static synchronized void bootstrap(MinecraftAdapter adapter) {
        if (instance != null) {
            throw new IllegalStateException(NAME + " is already bootstrapped");
        }
        PrimeLang.bind(adapter::translate);
        StellarClient client = new StellarClient(adapter);
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
        // Social is always-on infrastructure â€” keep the module enabled so settings sync.
        var socialHub = client.modules.get("social-hub");
        if (socialHub != null && !socialHub.isEnabled()) {
            socialHub.setEnabled(true);
        }
        var aiAssistant = client.modules.get("ai-assistant");
        if (aiAssistant != null && !aiAssistant.isEnabled()) {
            aiAssistant.setEnabled(true);
        }
        var customSkin = client.modules.get("custom-skin");
        if (customSkin != null && !customSkin.isEnabled()) {
            customSkin.setEnabled(true);
        }
        client.loadingOverlay.setStage(PrimeLang.get("prime.gui.loading.ready", "StellarClient ready"), 1f);
        LOGGER.info("{} v{} bootstrapped (Minecraft {}, {} modules, profile '{}'{})",
                NAME, dev.stellarclient.core.design.PrimeDesign.VERSION,
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
        social.tick();
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
        social.onWorldJoin();
        serverApi.onWorldJoin();
        eventBus.post(WorldJoinEvent.INSTANCE);
    }

    public void onWorldLeave() {
        crosshairProfiles.saveCurrentForServer(adapter.serverAddress());
        if (cloudSync.autoSync() && account.loggedIn()) {
            cloudSync.uploadNow(profiles.activeProfile());
        }
        presence.onWorldLeave();
        social.onWorldLeave();
        serverApi.onWorldLeave();
        eventBus.post(WorldLeaveEvent.INSTANCE);
    }

    public void shutdown() {
        voiceChat.shutdown();
        social.onWorldLeave();
        discordRpc.shutdown();
        profiles.saveActive();
        LOGGER.info("{} shut down, config saved", NAME);
    }

    public static StellarClient get() {
        StellarClient current = instance;
        if (current == null) {
            throw new IllegalStateException(NAME + " is not bootstrapped yet");
        }
        return current;
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
    public VoiceChatService voiceChat() { return voiceChat; }
    public PrimePresenceService presence() { return presence; }
    public SocialService social() { return social; }
    public ServerApiService serverApi() { return serverApi; }
    public AiAssistantService ai() { return ai; }
    public CustomSkinService customSkins() { return customSkins; }
}
