package dev.valerisclient.core;

import dev.valerisclient.core.modules.creator.CameraZoomModule;
import dev.valerisclient.core.modules.creator.CinematicCameraModule;
import dev.valerisclient.core.modules.creator.CinematicGridModule;
import dev.valerisclient.core.modules.creator.ClipBookmarkModule;
import dev.valerisclient.core.modules.creator.ClipToolsModule;
import dev.valerisclient.core.modules.creator.ReplayToolsModule;
import dev.valerisclient.core.modules.creator.ScreenshotModeModule;
import dev.valerisclient.core.modules.streamers.StreamBrandingModule;
import dev.valerisclient.core.modules.streamers.StreamChatRedactModule;
import dev.valerisclient.core.modules.streamers.StreamDebugShieldModule;
import dev.valerisclient.core.modules.streamers.StreamHudShieldModule;
import dev.valerisclient.core.modules.streamers.StreamNameMaskModule;
import dev.valerisclient.core.modules.streamers.StreamPrivacySuiteModule;
import dev.valerisclient.core.modules.performance.AdaptiveFpsModule;
import dev.valerisclient.core.modules.performance.AnimationOptimizerModule;
import dev.valerisclient.core.modules.performance.ChunkOptimizerModule;
import dev.valerisclient.core.modules.performance.DynamicFpsModule;
import dev.valerisclient.core.modules.performance.EntityCullingModule;
import dev.valerisclient.core.modules.performance.FastLoadingModule;
import dev.valerisclient.core.modules.performance.FpsBoosterModule;
import dev.valerisclient.core.modules.performance.MemoryMonitorModule;
import dev.valerisclient.core.modules.performance.MemorySpikeAlertModule;
import dev.valerisclient.core.modules.performance.ParticleOptimizerModule;
import dev.valerisclient.core.modules.performance.PerformanceProfilesModule;
import dev.valerisclient.core.modules.performance.RamCleanerModule;
import dev.valerisclient.core.modules.prime.ClientBadgeModule;
import dev.valerisclient.core.modules.prime.CustomSkinModule;
import dev.valerisclient.core.modules.prime.DiscordRichPresenceModule;
import dev.valerisclient.core.modules.prime.GameplayDnaModule;
import dev.valerisclient.core.modules.prime.ModuleBundlesModule;
import dev.valerisclient.core.modules.prime.PrimeAccountModule;
import dev.valerisclient.core.modules.prime.PrimeConfigCloudModule;
import dev.valerisclient.core.modules.prime.PrimeCosmeticsModule;
import dev.valerisclient.core.modules.prime.PrimeProfilesModule;
import dev.valerisclient.core.modules.prime.PrimeSettingsManagerModule;
import dev.valerisclient.core.modules.prime.ServerNotesModule;
import dev.valerisclient.core.modules.prime.SmartProfileModule;
import dev.valerisclient.core.modules.pvp.ArmorHudModule;
import dev.valerisclient.core.modules.pvp.ChorusCooldownModule;
import dev.valerisclient.core.modules.pvp.ComboCounterModule;
import dev.valerisclient.core.modules.pvp.ComboTimerModule;
import dev.valerisclient.core.modules.pvp.CoordinatesModule;
import dev.valerisclient.core.modules.pvp.CpsCounterModule;
import dev.valerisclient.core.modules.pvp.CpvpSupplyModule;
import dev.valerisclient.core.modules.pvp.CritIndicatorModule;
import dev.valerisclient.core.modules.pvp.CrosshairEditorModule;
import dev.valerisclient.core.modules.pvp.CrystalSupplyModule;
import dev.valerisclient.core.modules.pvp.DamageIndicatorModule;
import dev.valerisclient.core.modules.pvp.DirectionHudModule;
import dev.valerisclient.core.modules.pvp.DuelTimerModule;
import dev.valerisclient.core.modules.pvp.ElytraStatusModule;
import dev.valerisclient.core.modules.pvp.FoodLevelModule;
import dev.valerisclient.core.modules.pvp.FpsCounterModule;
import dev.valerisclient.core.modules.pvp.GappleCooldownModule;
import dev.valerisclient.core.modules.pvp.HealthAlertModule;
import dev.valerisclient.core.modules.pvp.HitColorModule;
import dev.valerisclient.core.modules.pvp.HitParticlesModule;
import dev.valerisclient.core.modules.pvp.ItemCooldownModule;
import dev.valerisclient.core.modules.pvp.KeystrokesModule;
import dev.valerisclient.core.modules.pvp.KnockbackIndicatorModule;
import dev.valerisclient.core.modules.pvp.MaceSmashModule;
import dev.valerisclient.core.modules.pvp.ObsidianSupplyModule;
import dev.valerisclient.core.modules.pvp.OffhandHudModule;
import dev.valerisclient.core.modules.pvp.PearlCooldownModule;
import dev.valerisclient.core.modules.pvp.PearlLandingMarkerModule;
import dev.valerisclient.core.modules.pvp.PingDisplayModule;
import dev.valerisclient.core.modules.pvp.PotionHudModule;
import dev.valerisclient.core.modules.pvp.ReachHudModule;
import dev.valerisclient.core.modules.pvp.ShieldBreakAlertModule;
import dev.valerisclient.core.modules.pvp.ShieldDurabilityModule;
import dev.valerisclient.core.modules.pvp.ShieldStatusModule;
import dev.valerisclient.core.modules.pvp.SpeedHudModule;
import dev.valerisclient.core.modules.pvp.StreakCounterModule;
import dev.valerisclient.core.modules.pvp.TargetHudModule;
import dev.valerisclient.core.modules.pvp.TntTimerModule;
import dev.valerisclient.core.modules.pvp.TotemAlertModule;
import dev.valerisclient.core.modules.pvp.TotemCounterModule;
import dev.valerisclient.core.modules.pvp.WindChargeCooldownModule;
import dev.valerisclient.core.modules.qol.AutoGgModule;
import dev.valerisclient.core.modules.qol.AutoJumpModule;
import dev.valerisclient.core.modules.qol.AutoRespawnModule;
import dev.valerisclient.core.modules.qol.BetterChatModule;
import dev.valerisclient.core.modules.qol.BetterTooltipsModule;
import dev.valerisclient.core.modules.qol.ChatFilterModule;
import dev.valerisclient.core.modules.qol.ChatTimestampModule;
import dev.valerisclient.core.modules.qol.DeathReplayModule;
import dev.valerisclient.core.modules.qol.DeathWaypointModule;
import dev.valerisclient.core.modules.qol.AlwaysDayModule;
import dev.valerisclient.core.modules.qol.FullbrightModule;
import dev.valerisclient.core.modules.qol.HandShaderModule;
import dev.valerisclient.core.modules.qol.LowFireModule;
import dev.valerisclient.core.modules.qol.NoRainModule;
import dev.valerisclient.core.modules.qol.InventorySearchModule;
import dev.valerisclient.core.modules.qol.ItemCounterModule;
import dev.valerisclient.core.modules.qol.MentionHighlightModule;
import dev.valerisclient.core.modules.qol.ServerSwitcherModule;
import dev.valerisclient.core.modules.qol.SessionRecapModule;
import dev.valerisclient.core.modules.qol.ShulkerPreviewModule;
import dev.valerisclient.core.modules.qol.TabAnimationModule;
import dev.valerisclient.core.modules.qol.ToggleSneakModule;
import dev.valerisclient.core.modules.qol.ToggleSprintModule;
import dev.valerisclient.core.modules.qol.WaypointsModule;
import dev.valerisclient.core.modules.qol.ZoomModule;
import dev.valerisclient.core.modules.survival.BaseRadiusModule;
import dev.valerisclient.core.modules.survival.BedReminderModule;
import dev.valerisclient.core.modules.survival.CropGrowthHudModule;
import dev.valerisclient.core.modules.survival.DateHudModule;
import dev.valerisclient.core.modules.survival.DayCounterHudModule;
import dev.valerisclient.core.modules.survival.DayTimeModule;
import dev.valerisclient.core.modules.survival.DeathCounterModule;
import dev.valerisclient.core.modules.survival.DepthHudModule;
import dev.valerisclient.core.modules.survival.ElytraFlightHudModule;
import dev.valerisclient.core.modules.survival.FriendDeathPingModule;
import dev.valerisclient.core.modules.survival.LightLevelModule;
import dev.valerisclient.core.modules.survival.MobSpawnSafeModule;
import dev.valerisclient.core.modules.survival.RaidAlertModule;
import dev.valerisclient.core.modules.survival.SaturationHudModule;
import dev.valerisclient.core.modules.survival.SpawnDistanceModule;
import dev.valerisclient.core.modules.survival.StructureLogModule;
import dev.valerisclient.core.modules.survival.TeamTagHudModule;
import dev.valerisclient.core.modules.survival.ToolDurabilityModule;
import dev.valerisclient.core.modules.survival.VillagerTradeLogModule;
import dev.valerisclient.core.modules.survival.WeatherHudModule;
import dev.valerisclient.core.modules.smp.AfkAlertModule;
import dev.valerisclient.core.modules.smp.BiomeCoordsModule;
import dev.valerisclient.core.modules.smp.ChunkCoordsModule;
import dev.valerisclient.core.modules.smp.DeathCostModule;
import dev.valerisclient.core.modules.smp.NetherLinkModule;
import dev.valerisclient.core.modules.smp.RepairAlertModule;
import dev.valerisclient.core.modules.smp.ServerSessionModule;
import dev.valerisclient.core.modules.smp.ShopWaypointModule;
import dev.valerisclient.core.modules.smp.SpawnCompassModule;
import dev.valerisclient.core.modules.smp.TravelEtaModule;

/**
 * Single registration point of every built-in Prime module.
 *
 * <p>Explicit list, no classpath scanning: registration order is the ClickGUI
 * display order, construction is reflection-free, and a module that fails to
 * compile fails the build instead of silently disappearing.</p>
 */
final class Modules {

    private Modules() {
    }

    static void registerBuiltins(ValerisClient client) {
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

        // Prime
        modules.register(new PrimeProfilesModule(client.profiles()));
        modules.register(new ModuleBundlesModule(modules, client.notifications()));
        modules.register(new SmartProfileModule(adapter, client.profiles(), client.notifications()));
        modules.register(new GameplayDnaModule(modules, client.notifications()));
        modules.register(new ServerNotesModule(adapter, client.notifications()));
        modules.register(new PrimeConfigCloudModule(client.cloudSync(), client.profiles()));
        modules.register(new PrimeCosmeticsModule(client.cosmetics()));
        modules.register(new PrimeAccountModule(hud, themes, adapter, client.account()));
        modules.register(new ClientBadgeModule(client.presence(), client.account()));
        modules.register(new CustomSkinModule(client.customSkins()));
        modules.register(new DiscordRichPresenceModule(client.discordRpc(), adapter, modules, client.account()));
        modules.register(new PrimeSettingsManagerModule(modules, adapter));
    }
}
