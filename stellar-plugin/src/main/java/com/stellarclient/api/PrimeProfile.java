package com.stellarclient.api;

import java.util.UUID;

/** Immutable snapshot of a Prime account on this server. */
public record PrimeProfile(
        UUID uuid,
        String username,
        boolean StellarClient,
        String clientVersion,
        long firstJoinMillis,
        long playtimeSeconds,
        int xp,
        int level
) {
}
