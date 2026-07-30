package dev.valarisclient.core.modules.streamers;

import dev.valarisclient.core.event.ClientTickEvent;
import dev.valarisclient.core.hud.HudElement;
import dev.valarisclient.core.hud.HudManager;
import dev.valarisclient.core.module.BooleanSetting;
import dev.valarisclient.core.module.Module;
import dev.valarisclient.core.module.ModuleCategory;
import dev.valarisclient.core.stream.StreamerPrivacyState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Hides location, server, and waypoint HUD elements that can leak on stream. */
public final class StreamHudShieldModule extends Module {

    private static final List<String> COORD_IDS = List.of(
            "coordinates", "chunk-coords", "biome-coords", "depth-hud",
            "spawn-distance", "nether-link", "spawn-compass");
    private static final List<String> SERVER_IDS = List.of(
            "server-session", "server-switcher", "ping", "valaris-account",
            "scoreboard-stats", "balance-hud", "team-tag-hud", "friend-death-ping", "reach-hud");
    private static final List<String> WAYPOINT_IDS = List.of(
            "waypoints", "death-waypoint", "shop-waypoint", "travel-eta",
            "base-radius", "structure-log", "pearl-landing-marker");

    private final BooleanSetting hideCoords =
            addSetting(new BooleanSetting(
                    "hide-coords", "Hide coords", "Hide coordinate-related HUD", true));
    private final BooleanSetting hideServerInfo =
            addSetting(new BooleanSetting(
                    "hide-server-info", "Hide server info", "Hide server and account HUD", true));
    private final BooleanSetting hideWaypoints =
            addSetting(new BooleanSetting(
                    "hide-waypoints", "Hide waypoints", "Hide waypoint and navigation HUD", true));

    private final HudManager hud;
    private final Set<String> hiddenIds = new HashSet<>();
    private final Set<String> savedVisible = new HashSet<>();

    public StreamHudShieldModule(HudManager hud) {
        super("stream-hud-shield", "Stream HUD Shield",
                "Hides sensitive HUD elements during streams", ModuleCategory.STREAMERS);
        this.hud = hud;
        listen(ClientTickEvent.class, event -> apply());
    }

    @Override
    protected void onEnable() {
        StreamerPrivacyState.setHudShield(true);
        StreamerPrivacyState.setBlockLocationHud(true);
        apply();
    }

    @Override
    protected void onDisable() {
        restore();
        StreamerPrivacyState.setHudShield(false);
        StreamerPrivacyState.setBlockLocationHud(false);
    }

    private void apply() {
        if (!isEnabled()) {
            return;
        }
        if (hideCoords.get()) {
            COORD_IDS.forEach(this::hideElement);
        }
        if (hideServerInfo.get()) {
            SERVER_IDS.forEach(this::hideElement);
        }
        if (hideWaypoints.get()) {
            WAYPOINT_IDS.forEach(this::hideElement);
        }
    }

    private void hideElement(String id) {
        if (hiddenIds.contains(id)) {
            return;
        }
        HudElement element = hud.get(id);
        if (element != null && element.isVisible()) {
            savedVisible.add(id);
            element.setVisible(false);
        }
        hiddenIds.add(id);
    }

    private void restore() {
        for (String id : savedVisible) {
            HudElement element = hud.get(id);
            if (element != null) {
                element.setVisible(true);
            }
        }
        savedVisible.clear();
        hiddenIds.clear();
    }
}
