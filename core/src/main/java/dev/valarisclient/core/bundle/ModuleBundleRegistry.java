package dev.valarisclient.core.bundle;

import java.util.List;

/** Built-in module bundle presets. */
public final class ModuleBundleRegistry {

    public static final ModuleBundle CPVP_KIT = new ModuleBundle(
            "cpvp-kit",
            "CPvP Kit",
            "Crystal PvP essentials",
            List.of(
                    "combo-counter", "combo-timer", "streak-counter", "target-hud",
                    "pearl-cooldown", "shield-status", "shield-break-alert", "crit-indicator",
                    "knockback-indicator", "health-alert", "totem-counter", "reach-hud",
                    "crystal-supply", "obsidian-supply", "cpvp-supply", "keystrokes", "cps"
            ));

    public static final ModuleBundle HARDCORE_SURVIVAL = new ModuleBundle(
            "hardcore-survival",
            "Hardcore Survival",
            "Survival and safety HUD",
            List.of(
                    "waypoints", "light-level", "mob-spawn-safe", "bed-reminder",
                    "death-waypoint", "crop-growth-hud", "raid-alert", "tool-durability",
                    "spawn-compass", "depth-hud", "death-counter", "structure-log"
            ));

    public static final ModuleBundle SPEEDRUN_LITE = new ModuleBundle(
            "speedrun-lite",
            "Speedrun Lite",
            "Minimal overlays for fast runs",
            List.of(
                    "coordinates", "fps", "toggle-sprint", "fullbright",
                    "spawn-distance", "depth-hud", "day-time", "ping"
            ));

    public static final ModuleBundle STREAMER_PACK = new ModuleBundle(
            "streamer-pack",
            "Streamer Pack",
            "Recording and stream-safe tools",
            List.of(
                    "stream-privacy-suite", "stream-debug-shield", "stream-chat-redact",
                    "stream-name-mask", "stream-hud-shield", "stream-branding",
                    "cinematic-camera", "cinematic-grid",
                    "clip-recorder", "clip-bookmark", "screenshot-mode", "replay-tools"
            ));

    private static final List<ModuleBundle> ALL = List.of(
            CPVP_KIT, HARDCORE_SURVIVAL, SPEEDRUN_LITE, STREAMER_PACK);

    private ModuleBundleRegistry() {
    }

    public static List<ModuleBundle> all() {
        return ALL;
    }

    public static ModuleBundle byId(String id) {
        for (ModuleBundle bundle : ALL) {
            if (bundle.id().equals(id)) {
                return bundle;
            }
        }
        return null;
    }
}
