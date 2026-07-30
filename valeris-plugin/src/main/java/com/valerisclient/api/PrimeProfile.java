package com.valerisclient.api;

import java.util.UUID;

/** Immutable snapshot of a Prime account on this server. */
public record PrimeProfile(
        UUID uuid,
        String username,
        boolean ValerisClient,
        String clientVersion,
        long firstJoinMillis,
        long playtimeSeconds,
        int xp,
        int level
) {
}
