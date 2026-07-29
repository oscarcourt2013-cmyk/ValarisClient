package com.stellarclient.listeners;

import com.stellarclient.detection.ClientDetectionService;
import com.stellarclient.missions.MissionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class MissionListener implements Listener {

    private final MissionService missions;
    private final ClientDetectionService detection;

    public MissionListener(MissionService missions, ClientDetectionService detection) {
        this.missions = missions;
        this.detection = detection;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (detection.isVerified(event.getPlayer().getUniqueId())) {
            missions.onBlockBreak(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && detection.isVerified(killer.getUniqueId())) {
            missions.onPlayerKill(killer);
        }
    }
}
