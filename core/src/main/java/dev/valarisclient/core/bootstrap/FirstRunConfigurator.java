package dev.valarisclient.core.bootstrap;

import dev.valarisclient.core.ValarisClient;
import dev.valarisclient.core.bundle.ModuleBundle;
import dev.valarisclient.core.bundle.ModuleBundleRegistry;
import dev.valarisclient.core.gui.FavoritesManager;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleManager;
import dev.valarisclient.core.profile.ProfileManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Curated starter modules and favorites for first launch / profile presets. */
public final class FirstRunConfigurator {

    private static final List<String> CORE = List.of(
            "fps-counter", "discord-rpc", "valaris-cosmetics", "social-hub"
    );

    private static final List<String> PVP_EXTRA = List.of(
            "target-hud", "combo-counter", "combo-timer", "streak-counter",
            "hit-color", "armor-hud", "potion-hud", "mace-smash", "pearl-cooldown",
            "pearl-landing-marker", "totem-counter", "reach-hud", "shield-status",
            "shield-break-alert", "health-alert", "crit-indicator", "knockback-indicator"
    );

    private static final List<String> SURVIVAL_EXTRA = List.of(
            "waypoints", "auto-respawn", "toggle-sprint", "death-waypoint",
            "day-time", "depth-hud", "light-level", "mob-spawn-safe", "crop-growth-hud",
            "spawn-compass", "biome-coords", "spawn-distance", "tool-durability",
            "fullbright", "bed-reminder", "raid-alert", "structure-log"
    );

    private FirstRunConfigurator() {
    }

    public static void applyStarter(ValarisClient client) {
        applyPreset(client.modules(), client.favorites(), "default");
        seedGameplayProfiles(client);
        client.notifications().success("ValarisClient",
                "HUD premium activé — Right Shift pour le menu");
    }

    /** Writes default, pvp and survival profile files on first install. */
    public static void seedGameplayProfiles(ValarisClient client) {
        ProfileManager profiles = client.profiles();
        String original = profiles.activeProfile();
        profiles.saveActive();

        for (String preset : List.of("pvp", "survival")) {
            profiles.switchTo(preset);
            applyPreset(client.modules(), client.favorites(), preset);
            profiles.saveActive();
        }

        if (!original.equals(profiles.activeProfile())) {
            profiles.switchTo(original);
        }
    }

    public static void applyPreset(ModuleManager modules, FavoritesManager favorites, String preset) {
        Set<String> ids = new LinkedHashSet<>(CORE);
        if ("pvp".equals(preset)) {
            ids.addAll(PVP_EXTRA);
            applyBundle(modules, ModuleBundleRegistry.CPVP_KIT);
        } else if ("survival".equals(preset)) {
            ids.addAll(SURVIVAL_EXTRA);
            applyBundle(modules, ModuleBundleRegistry.HARDCORE_SURVIVAL);
        }
        for (String id : ids) {
            enable(modules, id);
        }
        if (favorites != null) {
            seedFavorites(favorites, preset);
        }
    }

    /** Enables all modules in a bundle preset. */
    public static void applyBundle(ModuleManager modules, ModuleBundle bundle) {
        for (String id : bundle.moduleIds()) {
            enable(modules, id);
        }
    }

    private static void enable(ModuleManager modules, String id) {
        Module module = modules.get(id);
        if (module != null && !module.isEnabled()) {
            module.setEnabled(true);
        }
    }

    private static void seedFavorites(FavoritesManager favorites, String preset) {
        favorites.clear();
        favorites.add("crosshair-editor");
        favorites.add("discord-rpc");
        favorites.add("fps-counter");
        favorites.add("keystrokes");
        favorites.add("module-bundles");
        if ("pvp".equals(preset)) {
            favorites.add("target-hud");
            favorites.add("combo-counter");
            favorites.add("combo-timer");
            favorites.add("mace-smash");
            favorites.add("pearl-cooldown");
        } else if ("survival".equals(preset)) {
            favorites.add("waypoints");
            favorites.add("day-time");
            favorites.add("spawn-compass");
            favorites.add("mob-spawn-safe");
            favorites.add("fullbright");
        } else {
            favorites.add("zoom");
            favorites.add("valaris-cosmetics");
        }
    }
}
