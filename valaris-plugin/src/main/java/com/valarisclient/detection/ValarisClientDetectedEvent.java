package com.valarisclient.detection;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired on the main thread after a successful ValarisClient handshake. */
public final class ValarisClientDetectedEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String clientVersion;
    private final boolean firstDetection;

    public ValarisClientDetectedEvent(Player player, String clientVersion, boolean firstDetection) {
        super(player);
        this.clientVersion = clientVersion;
        this.firstDetection = firstDetection;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public boolean isFirstDetection() {
        return firstDetection;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
