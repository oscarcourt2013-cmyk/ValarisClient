package com.stellarclient.api;

import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/** Public API for other plugins. */
public interface StellarClientAPI {

    boolean isStellarClient(Player player);

    boolean isStellarClient(UUID uuid);

    Optional<String> getClientVersion(Player player);

    Optional<PrimeProfile> getPrimeProfile(Player player);

    Optional<PrimeProfile> getPrimeProfile(UUID uuid);

    void addPrimeXP(UUID uuid, int amount);

    void addPrimeXP(UUID uuid, int amount, String reason);
}
