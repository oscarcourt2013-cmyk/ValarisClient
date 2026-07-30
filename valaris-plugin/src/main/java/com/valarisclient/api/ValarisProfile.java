package com.valarisclient.api;

import java.util.UUID;

/** Immutable snapshot of a Valaris account on this server. */
public record ValarisProfile(
        UUID uuid,
        String username,
        boolean ValarisClient,
        String clientVersion,
        long firstJoinMillis,
        long playtimeSeconds,
        int xp,
        int level
) {
}
