package com.stellarclient.database;

import com.stellarclient.api.PrimeProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Database {

    void init();

    void close();

    void upsertPlayer(UUID uuid, String username);

    void markStellarClient(UUID uuid, String username, String clientVersion, long nowMillis);

    Optional<PrimeProfile> findProfile(UUID uuid);

    void addXp(UUID uuid, int amount);

    void setPlaytime(UUID uuid, long playtimeSeconds);

    long getPlaytime(UUID uuid);

    boolean claimReward(UUID uuid, String rewardId);

    boolean hasReward(UUID uuid, String rewardId);

    boolean unlockAchievement(UUID uuid, String achievementId, long nowMillis);

    boolean hasAchievement(UUID uuid, String achievementId);

    List<String> listAchievements(UUID uuid);

    void setMissionProgress(UUID uuid, String missionId, int progress);

    int getMissionProgress(UUID uuid, String missionId);

    void completeMission(UUID uuid, String missionId, long nowMillis);

    boolean isMissionComplete(UUID uuid, String missionId);
}
