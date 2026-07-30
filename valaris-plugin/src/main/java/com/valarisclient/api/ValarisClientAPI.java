package com.valarisclient.api;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/** Public API for other plugins. */
public interface ValarisClientAPI {

    boolean isValarisClient(Player player);

    boolean isValarisClient(UUID uuid);

    Optional<String> getClientVersion(Player player);

    Optional<ValarisProfile> getPrimeProfile(Player player);

    Optional<ValarisProfile> getPrimeProfile(UUID uuid);

    void addPrimeXP(UUID uuid, int amount);

    void addPrimeXP(UUID uuid, int amount, String reason);
}
