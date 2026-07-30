package com.valarisclient.api;

import com.valarisclient.database.Database;
import com.valarisclient.detection.ClientDetectionService;
import com.valarisclient.xp.XpService;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public final class ValarisClientAPIImpl implements ValarisClientAPI {

    private final ClientDetectionService detection;
    private final Database database;
    private final XpService xp;

    public ValarisClientAPIImpl(ClientDetectionService detection, Database database, XpService xp) {
        this.detection = detection;
        this.database = database;
        this.xp = xp;
    }

    @Override
    public boolean isValarisClient(Player player) {
        return player != null && detection.isVerified(player.getUniqueId());
    }

    @Override
    public boolean isValarisClient(UUID uuid) {
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
    public Optional<ValarisProfile> getPrimeProfile(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        return getPrimeProfile(player.getUniqueId());
    }

    @Override
    public Optional<ValarisProfile> getPrimeProfile(UUID uuid) {
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
