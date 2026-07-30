package dev.valarisclient.core;

import dev.valarisclient.core.modules.creator.CameraZoomModule;
import dev.valarisclient.core.modules.creator.CinematicCameraModule;
import dev.valarisclient.core.modules.creator.CinematicGridModule;
import dev.valarisclient.core.modules.creator.ClipBookmarkModule;
import dev.valarisclient.core.modules.creator.ClipToolsModule;
import dev.valarisclient.core.modules.creator.ReplayToolsModule;
import dev.valarisclient.core.modules.creator.ScreenshotModeModule;
import dev.valarisclient.core.modules.streamers.StreamBrandingModule;
import dev.valarisclient.core.modules.streamers.StreamChatRedactModule;
import dev.valarisclient.core.modules.streamers.StreamDebugShieldModule;
import dev.valarisclient.core.modules.streamers.StreamHudShieldModule;
import dev.valarisclient.core.modules.streamers.StreamNameMaskModule;
import dev.valarisclient.core.modules.streamers.StreamPrivacySuiteModule;
import dev.valarisclient.core.modules.performance.AdaptiveFpsModule;
import dev.valarisclient.core.modules.performance.AnimationOptimizerModule;
import dev.valarisclient.core.modules.performance.ChunkOptimizerModule;
import dev.valarisclient.core.modules.performance.DynamicFpsModule;
import dev.valarisclient.core.modules.performance.EntityCullingModule;
import dev.valarisclient.core.modules.performance.FastLoadingModule;
import dev.valarisclient.core.modules.performance.FpsBoosterModule;
import dev.valarisclient.core.modules.performance.MemoryMonitorModule;
import dev.valarisclient.core.modules.performance.MemorySpikeAlertModule;
import dev.valarisclient.core.modules.performance.ParticleOptimizerModule;
import dev.valarisclient.core.modules.performance.PerformanceProfilesModule;
import dev.valarisclient.core.modules.performance.RamCleanerModule;
import dev.valarisclient.core.modules.valaris.ClientBadgeModule;
import dev.valarisclient.core.modules.valaris.CustomSkinModule;
import dev.valarisclient.core.modules.valaris.DiscordRichPresenceModule;
import dev.valarisclient.core.modules.valaris.GameplayDnaModule;
import dev.valarisclient.core.modules.valaris.ModuleBundlesModule;
import dev.valarisclient.core.modules.valaris.ValarisAccountModule;
import dev.valarisclient.core.modules.valaris.ValarisConfigCloudModule;
import dev.valarisclient.core.modules.valaris.ValarisCosmeticsModule;
import dev.valarisclient.core.modules.valaris.ValarisProfilesModule;
import dev.valarisclient.core.modules.valaris.ValarisSettingsManagerModule;
import dev.valarisclient.core.modules.valaris.ServerNotesModule;
import dev.valarisclient.core.modules.valaris.SmartProfileModule;
import dev.valarisclient.core.modules.pvp.ArmorHudModule;
import dev.valarisclient.core.modules.pvp.ChorusCooldownModule;
import dev.valarisclient.core.modules.pvp.ComboCounterModule;
import dev.valarisclient.core.modules.pvp.ComboTimerModule;
import dev.valarisclient.core.modules.pvp.CoordinatesModule;
import dev.valarisclient.core.modules.pvp.CpsCounterModule;
import dev.valarisclient.core.modules.pvp.CpvpSupplyModule;
import dev.valarisclient.core.modules.pvp.CritIndicatorModule;
import dev.valarisclient.core.modules.pvp.CrosshairEditorModule;
import dev.valarisclient.core.modules.pvp.CrystalSupplyModule;
import dev.valarisclient.core.modules.pvp.DamageIndicatorModule;
import dev.valarisclient.core.modules.pvp.DirectionHudModule;
import dev.valarisclient.core.modules.pvp.DuelTimerModule;
import dev.valarisclient.core.modules.pvp.ElytraStatusModule;
import dev.valarisclient.core.modules.pvp.FoodLevelModule;
import dev.valarisclient.core.modules.pvp.FpsCounterModule;
import dev.valarisclient.core.modules.pvp.GappleCooldownModule;
import dev.valarisclient.core.modules.pvp.HealthAlertModule;
import dev.valarisclient.core.modules.pvp.HitColorModule;
import dev.valarisclient.core.modules.pvp.HitParticlesModule;
import dev.valarisclient.core.modules.pvp.ItemCooldownModule;
import dev.valarisclient.core.modules.pvp.KeystrokesModule;
import dev.valarisclient.core.modules.pvp.KnockbackIndicatorModule;
import dev.valarisclient.core.modules.pvp.MaceSmashModule;
import dev.valarisclient.core.modules.pvp.ObsidianSupplyModule;
import dev.valarisclient.core.modules.pvp.OffhandHudModule;
import dev.valarisclient.core.modules.pvp.PearlCooldownModule;
import dev.valarisclient.core.modules.pvp.PearlLandingMarkerModule;
import dev.valarisclient.core.modules.pvp.PingDisplayModule;
import dev.valarisclient.core.modules.pvp.PotionHudModule;
import dev.valarisclient.core.modules.pvp.ReachHudModule;
import dev.valarisclient.core.modules.pvp.ShieldBreakAlertModule;
import dev.valarisclient.core.modules.pvp.ShieldDurabilityModule;
import dev.valarisclient.core.modules.pvp.ShieldStatusModule;
import dev.valarisclient.core.modules.pvp.SpeedHudModule;
import dev.valarisclient.core.modules.pvp.StreakCounterModule;
import dev.valarisclient.core.modules.pvp.TargetHudModule;
import dev.valarisclient.core.modules.pvp.TntTimerModule;
import dev.valarisclient.core.modules.pvp.TotemAlertModule;
import dev.valarisclient.core.modules.pvp.TotemCounterModule;
import dev.valarisclient.core.modules.pvp.WindChargeCooldownModule;
import dev.valarisclient.core.modules.qol.AutoGgModule;
import dev.valarisclient.core.modules.qol.AutoJumpModule;
import dev.valarisclient.core.modules.qol.AutoRespawnModule;
import dev.valarisclient.core.modules.qol.BetterChatModule;
import dev.valarisclient.core.modules.qol.BetterTooltipsModule;
import dev.valarisclient.core.modules.qol.ChatFilterModule;
import dev.valarisclient.core.modules.qol.ChatTimestampModule;
import dev.valarisclient.core.modules.qol.DeathReplayModule;
import dev.valarisclient.core.modules.qol.DeathWaypointModule;
import dev.valarisclient.core.modules.qol.AlwaysDayModule;
import dev.valarisclient.core.modules.qol.FullbrightModule;
import dev.valarisclient.core.modules.qol.HandShaderModule;
import dev.valarisclient.core.modules.qol.LowFireModule;
import dev.valarisclient.core.modules.qol.NoRainModule;
import dev.valarisclient.core.modules.qol.InventorySearchModule;
import dev.valarisclient.core.modules.qol.ItemCounterModule;
import dev.valarisclient.core.modules.qol.MentionHighlightModule;
import dev.valarisclient.core.modules.qol.ServerSwitcherModule;
import dev.valarisclient.core.modules.qol.SessionRecapModule;
import dev.valarisclient.core.modules.qol.ShulkerPreviewModule;
import dev.valarisclient.core.modules.qol.TabAnimationModule;
import dev.valarisclient.core.modules.qol.ToggleSneakModule;
import dev.valarisclient.core.modules.qol.ToggleSprintModule;
import dev.valarisclient.core.modules.qol.WaypointsModule;
import dev.valarisclient.core.modules.qol.ZoomModule;
import dev.valarisclient.core.modules.survival.BaseRadiusModule;
import dev.valarisclient.core.modules.survival.BedReminderModule;
import dev.valarisclient.core.modules.survival.CropGrowthHudModule;
import dev.valarisclient.core.modules.survival.DateHudModule;
import dev.valarisclient.core.modules.survival.DayCounterHudModule;
import dev.valarisclient.core.modules.survival.DayTimeModule;
import dev.valarisclient.core.modules.survival.DeathCounterModule;
import dev.valarisclient.core.modules.survival.DepthHudModule;
import dev.valarisclient.core.modules.survival.ElytraFlightHudModule;
import dev.valarisclient.core.modules.survival.FriendDeathPingModule;
import dev.valarisclient.core.modules.survival.LightLevelModule;
import dev.valarisclient.core.modules.survival.MobSpawnSafeModule;
import dev.valarisclient.core.modules.survival.RaidAlertModule;
import dev.valarisclient.core.modules.survival.SaturationHudModule;
import dev.valarisclient.core.modules.survival.SpawnDistanceModule;
import dev.valarisclient.core.modules.survival.StructureLogModule;
import dev.valarisclient.core.modules.survival.TeamTagHudModule;
import dev.valarisclient.core.modules.survival.ToolDurabilityModule;
import dev.valarisclient.core.modules.survival.VillagerTradeLogModule;
import dev.valarisclient.core.modules.survival.WeatherHudModule;
import dev.valarisclient.core.modules.smp.AfkAlertModule;
import dev.valarisclient.core.modules.smp.BiomeCoordsModule;
import dev.valarisclient.core.modules.smp.ChunkCoordsModule;
import dev.valarisclient.core.modules.smp.DeathCostModule;
import dev.valarisclient.core.modules.smp.NetherLinkModule;
import dev.valarisclient.core.modules.smp.RepairAlertModule;
import dev.valarisclient.core.modules.smp.ServerSessionModule;
import dev.valarisclient.core.modules.smp.ShopWaypointModule;
import dev.valarisclient.core.modules.smp.SpawnCompassModule;
import dev.valarisclient.core.modules.smp.TravelEtaModule;

/**
 * Single registration point of every built-in Valaris module.
 *
 * <p>Explicit list, no classpath scanning: registration order is the ClickGUI
 * display order, construction is reflection-free, and a module that fails to
 * compile fails the build instead of silently disappearing.</p>
 */
final class Modules {

    private Modules() {
    }

    static void registerBuiltins(ValarisClient client) {
        var modules = client.modules();
        var hud = client.hud();
        var themes = client.themes();
        var adapter = client.adapter();

        // PvP (40)
        modules.register(new KeystrokesModule(hud, themes, adapter));
        modules.register(new CpsCounterModule(hud, themes, adapter));
        modules.register(new FpsCounterModule(hud, themes, adapter));
        modules.register(new PingDisplayModule(hud, themes, adapter));
        modules.register(new ComboCounterModule(hud, themes, adapter));
        modules.register(new ComboTimerModule(hud, themes, adapter));
        modules.register(new StreakCounterModule(hud, themes, adapter));
        modules.register(new TargetHudModule(hud, themes, adapter));
        modules.register(new ArmorHudModule(hud, themes, adapter));
        modules.register(new PotionHudModule(hud, themes, adapter));
        modules.register(new ItemCooldownModule(hud, themes, adapter));
        modules.register(new CrosshairEditorModule(hud, themes, adapter, client.crosshairConfig(),
                client.crosshairPresets(), client.crosshairProfiles()));
        modules.register(new HitColorModule());
        modules.register(new HitParticlesModule(adapter));
        modules.register(new DamageIndicatorModule(hud, themes, adapter));
        modules.register(new DirectionHudModule(hud, themes, adapter));
        modules.register(new CoordinatesModule(hud, themes, adapter));
        modules.register(new SpeedHudModule(hud, themes, adapter));
        modules.register(new MaceSmashModule(hud, themes, adapter));
        modules.register(new ShieldStatusModule(hud, themes, adapter));
        modules.register(new ShieldDurabilityModule(hud, themes, adapter));
        modules.register(new ShieldBreakAlertModule(adapter, client.notifications()));
        modules.register(new PearlCooldownModule(hud, themes, adapter));
        modules.register(new PearlLandingMarkerModule(hud, themes, adapter));
        modules.register(new GappleCooldownModule(hud, themes, adapter));
        modules.register(new WindChargeCooldownModule(hud, themes, adapter));
        modules.register(new ChorusCooldownModule(hud, themes, adapter));
        modules.register(new TotemCounterModule(hud, themes, adapter));
        modules.register(new ReachHudModule(hud, themes, adapter));
        modules.register(new CritIndicatorModule(hud, themes, adapter));
        modules.register(new KnockbackIndicatorModule(hud, themes, adapter));
        modules.register(new DuelTimerModule(hud, themes, adapter));
        modules.register(new OffhandHudModule(hud, themes, adapter));
        modules.register(new FoodLevelModule(hud, themes, adapter));
        modules.register(new ObsidianSupplyModule(hud, themes, adapter));
        modules.register(new CrystalSupplyModule(hud, themes, adapter));
        modules.register(new CpvpSupplyModule(hud, themes, adapter));
        modules.register(new ElytraStatusModule(hud, themes, adapter));
        modules.register(new HealthAlertModule(adapter, client.notifications()));
        modules.register(new TotemAlertModule(adapter, client.notifications()));
        modules.register(new TntTimerModule(hud, themes, adapter));

        // Survival (32)
        modules.register(new DayTimeModule(hud, themes, adapter));
        modules.register(new DayCounterHudModule(hud, themes, adapter));
        modules.register(new DateHudModule(hud, themes));
        modules.register(new WeatherHudModule(hud, themes, adapter));
        modules.register(new LightLevelModule(hud, themes, adapter));
        modules.register(new MobSpawnSafeModule(hud, themes, adapter));
        modules.register(new CropGrowthHudModule(hud, themes, adapter));
        modules.register(new RaidAlertModule(adapter, client.notifications()));
        modules.register(new ElytraFlightHudModule(hud, themes, adapter));
        modules.register(new StructureLogModule(hud, themes, adapter));
        modules.register(new VillagerTradeLogModule(hud, themes));
        modules.register(new TeamTagHudModule(hud, themes, adapter));
        modules.register(new BaseRadiusModule(adapter, client.notifications()));
        modules.register(new FriendDeathPingModule(client.notifications()));
        modules.register(new DepthHudModule(hud, themes, adapter));
        modules.register(new ToolDurabilityModule(hud, themes, adapter));
        modules.register(new SaturationHudModule(hud, themes, adapter));
        modules.register(new SpawnDistanceModule(hud, themes, adapter));
        modules.register(new DeathCounterModule(hud, themes));
        modules.register(new BedReminderModule(adapter, client.notifications()));
        modules.register(new WaypointsModule(hud, themes, adapter));
        modules.register(new DeathWaypointModule(hud, themes, adapter));
        modules.register(new ShopWaypointModule(hud, themes, adapter));
        modules.register(new DeathCostModule(hud, themes, adapter));
        modules.register(new BiomeCoordsModule(hud, themes, adapter));
        modules.register(new SpawnCompassModule(hud, themes, adapter));
        modules.register(new ChunkCoordsModule(hud, themes, adapter));
        modules.register(new NetherLinkModule(hud, themes, adapter));
        modules.register(new TravelEtaModule(hud, themes, adapter));
        modules.register(new ServerSessionModule(hud, themes, adapter));
        modules.register(new AfkAlertModule(adapter, client.notifications()));
        modules.register(new RepairAlertModule(adapter, client.notifications()));

        // Performance (12)
        modules.register(new FpsBoosterModule(adapter));
        modules.register(new EntityCullingModule(adapter));
        modules.register(new ParticleOptimizerModule(adapter));
        modules.register(new MemoryMonitorModule(hud, themes, adapter));
        modules.register(new MemorySpikeAlertModule(adapter, client.notifications()));
        modules.register(new AdaptiveFpsModule(adapter));
        modules.register(new RamCleanerModule(adapter));
        modules.register(new DynamicFpsModule(adapter));
        modules.register(new ChunkOptimizerModule(adapter));
        modules.register(new AnimationOptimizerModule(adapter));
        modules.register(new FastLoadingModule(adapter));
        modules.register(new PerformanceProfilesModule(adapter));

        // QoL (23)
        modules.register(new ZoomModule(adapter));
        modules.register(new FullbrightModule(adapter));
        modules.register(new NoRainModule());
        modules.register(new AlwaysDayModule());
        modules.register(new LowFireModule());
        modules.register(new HandShaderModule());
        modules.register(new AutoJumpModule(adapter));
        modules.register(new ToggleSprintModule(adapter));
        modules.register(new ToggleSneakModule(adapter));
        modules.register(new AutoRespawnModule(adapter));
        modules.register(new AutoGgModule(adapter));
        modules.register(new SessionRecapModule(adapter, client.notifications()));
        modules.register(new MentionHighlightModule(adapter));
        modules.register(new DeathReplayModule(adapter, client.notifications()));
        modules.register(new ChatTimestampModule());
        modules.register(new BetterChatModule());
        modules.register(new ChatFilterModule());
        modules.register(new TabAnimationModule());
        modules.register(new ItemCounterModule(hud, themes, adapter));
        modules.register(new ShulkerPreviewModule(hud, themes, adapter));
        modules.register(new BetterTooltipsModule(hud, themes, adapter));
        modules.register(new InventorySearchModule(hud, themes, adapter));
        modules.register(new ServerSwitcherModule(hud, themes, adapter));

        // Streamers (6)
        modules.register(new StreamPrivacySuiteModule(modules));
        modules.register(new StreamDebugShieldModule());
        modules.register(new StreamChatRedactModule());
        modules.register(new StreamNameMaskModule());
        modules.register(new StreamHudShieldModule(hud));
        modules.register(new StreamBrandingModule(hud));
        client.keybinds().rebind(client.keybinds().get("module.stream-privacy-suite"), 299);

        // Creator (7)
        modules.register(new CinematicCameraModule(hud, themes, adapter));
        modules.register(new CinematicGridModule(hud));
        modules.register(new ScreenshotModeModule(adapter));
        modules.register(new CameraZoomModule(adapter));
        modules.register(new ReplayToolsModule(hud, themes, adapter, client.replaySession(), client.replayStorage()));
        modules.register(new ClipToolsModule(hud, themes, adapter, client.clipRecorder(), client.keybinds()));
        modules.register(new ClipBookmarkModule(client.clipRecorder(), client.replaySession(),
                client.keybinds(), client.notifications()));

        // Valaris
        modules.register(new ValarisProfilesModule(client.profiles()));
        modules.register(new ModuleBundlesModule(modules, client.notifications()));
        modules.register(new SmartProfileModule(adapter, client.profiles(), client.notifications()));
        modules.register(new GameplayDnaModule(modules, client.notifications()));
        modules.register(new ServerNotesModule(adapter, client.notifications()));
        modules.register(new ValarisConfigCloudModule(client.cloudSync(), client.profiles()));
        modules.register(new ValarisCosmeticsModule(client.cosmetics()));
        modules.register(new ValarisAccountModule(hud, themes, adapter, client.account()));
        modules.register(new ClientBadgeModule(client.presence(), client.account()));
        modules.register(new CustomSkinModule(client.customSkins()));
        modules.register(new DiscordRichPresenceModule(client.discordRpc(), adapter, modules, client.account()));
        modules.register(new ValarisSettingsManagerModule(modules, adapter));
    }
}
