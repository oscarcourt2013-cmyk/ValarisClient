package com.stellarclient.api;

import com.stellarclient.database.Database;
import com.stellarclient.detection.ClientDetectionService;
import com.stellarclient.xp.XpService;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public final class StellarClientAPIImpl implements StellarClientAPI {

    private final ClientDetectionService detection;
    private final Database database;
    private final XpService xp;

    public StellarClientAPIImpl(ClientDetectionService detection, Database database, XpService xp) {
        this.detection = detection;
        this.database = database;
        this.xp = xp;
    }

    @Override
    public boolean isStellarClient(Player player) {
        return player != null && detection.isVerified(player.getUniqueId());
    }

    @Override
    public boolean isStellarClient(UUID uuid) {
        return uuid != null && detection.isVerified(uuid);
    }

    @Override
    public Optional<String> getClientVersion(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return detection.getVersion(player.getUniqueId());
    }

    @Override
    public Optional<PrimeProfile> getPrimeProfile(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return getPrimeProfile(player.getUniqueId());
    }

    @Override
    public Optional<PrimeProfile> getPrimeProfile(UUID uuid) {
        return database.findProfile(uuid);
    }

    @Override
    public void addPrimeXP(UUID uuid, int amount) {
        addPrimeXP(uuid, amount, "API");
    }

    @Override
    public void addPrimeXP(UUID uuid, int amount, String reason) {
        xp.addXp(uuid, amount, reason);
    }
}
