package com.valarisclient.database;

import com.valarisclient.api.ValarisProfile;
import com.valarisclient.utils.Text;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqlDatabase implements Database {

    private final HikariDataSource dataSource;
    private final boolean sqlite;

    public SqlDatabase(HikariDataSource dataSource, boolean sqlite) {
        this.dataSource = dataSource;
        this.sqlite = sqlite;
    }

    @Override
    public void init() {
        try (Connection c = dataSource.getConnection()) {
            DatabaseFactory.createSchema(c, sqlite);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to init ValarisClient database", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

    @Override
    public void upsertPlayer(UUID uuid, String username) {
        String sql = sqlite
                ? """
                  INSERT INTO players (uuid, username, first_join)
                  VALUES (?, ?, ?)
                  ON CONFLICT(uuid) DO UPDATE SET username = excluded.username
                  """
                : """
                  INSERT INTO players (uuid, username, first_join)
                  VALUES (?, ?, ?)
                  ON DUPLICATE KEY UPDATE username = VALUES(username)
                  """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void markValarisClient(UUID uuid, String username, String clientVersion, long nowMillis) {
        upsertPlayer(uuid, username);
        String sql = sqlite
                ? """
                  UPDATE players SET
                    valaris_client = 1,
                    client_version = ?,
                    username = ?,
                    first_join = CASE WHEN first_join = 0 THEN ? ELSE first_join END
                  WHERE uuid = ?
                  """
                : """
                  UPDATE players SET
                    valaris_client = 1,
                    client_version = ?,
                    username = ?,
                    first_join = IF(first_join = 0, ?, first_join)
                  WHERE uuid = ?
                  """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, clientVersion == null ? "" : clientVersion);
            ps.setString(2, username);
            ps.setLong(3, nowMillis);
            ps.setString(4, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<ValarisProfile> findProfile(UUID uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                int xp = rs.getInt("xp");
                return Optional.of(new ValarisProfile(
                        uuid,
                        rs.getString("username"),
                        rs.getInt("valaris_client") == 1,
                        rs.getString("client_version"),
                        rs.getLong("first_join"),
                        rs.getLong("playtime_seconds"),
                        xp,
                        Math.max(rs.getInt("level"), Text.levelForXp(xp))
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void addXp(UUID uuid, int amount) {
        if (amount == 0) {
            return;
        }
        String sql = "UPDATE players SET xp = xp + ?, level = ? WHERE uuid = ?";
        try (Connection c = dataSource.getConnection()) {
            int current = 0;
            try (PreparedStatement get = c.prepareStatement("SELECT xp FROM players WHERE uuid = ?")) {
                get.setString(1, uuid.toString());
                try (ResultSet rs = get.executeQuery()) {
                    if (rs.next()) {
                        current = rs.getInt(1);
                    }
                }
            }
            int nextXp = Math.max(0, current + amount);
            int level = Text.levelForXp(nextXp);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, amount);
                ps.setInt(2, level);
                ps.setString(3, uuid.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setPlaytime(UUID uuid, long playtimeSeconds) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE players SET playtime_seconds = ? WHERE uuid = ?")) {
            ps.setLong(1, playtimeSeconds);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public long getPlaytime(UUID uuid) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT playtime_seconds FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean claimReward(UUID uuid, String rewardId) {
        if (hasReward(uuid, rewardId)) {
            return false;
        }
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO rewards (uuid, reward, claimed_at) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, rewardId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean hasReward(UUID uuid, String rewardId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM rewards WHERE uuid = ? AND reward = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean unlockAchievement(UUID uuid, String achievementId, long nowMillis) {
        if (hasAchievement(uuid, achievementId)) {
            return false;
        }
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO achievements (uuid, achievement, unlocked_at) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, achievementId);
            ps.setLong(3, nowMillis);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean hasAchievement(UUID uuid, String achievementId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM achievements WHERE uuid = ? AND achievement = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, achievementId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<String> listAchievements(UUID uuid) {
        List<String> out = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT achievement FROM achievements WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    @Override
    public void setMissionProgress(UUID uuid, String missionId, int progress) {
        String sql = sqlite
                ? """
                  INSERT INTO missions (uuid, mission, progress, completed_at)
                  VALUES (?, ?, ?, 0)
                  ON CONFLICT(uuid, mission) DO UPDATE SET progress = excluded.progress
                  """
                : """
                  INSERT INTO missions (uuid, mission, progress, completed_at)
                  VALUES (?, ?, ?, 0)
                  ON DUPLICATE KEY UPDATE progress = VALUES(progress)
                  """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, missionId);
            ps.setInt(3, progress);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getMissionProgress(UUID uuid, String missionId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT progress FROM missions WHERE uuid = ? AND mission = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, missionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void completeMission(UUID uuid, String missionId, long nowMillis) {
        String sql = sqlite
                ? """
                  INSERT INTO missions (uuid, mission, progress, completed_at)
                  VALUES (?, ?, 0, ?)
                  ON CONFLICT(uuid, mission) DO UPDATE SET completed_at = excluded.completed_at
                  """
                : """
                  INSERT INTO missions (uuid, mission, progress, completed_at)
                  VALUES (?, ?, 0, ?)
                  ON DUPLICATE KEY UPDATE completed_at = VALUES(completed_at)
                  """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, missionId);
            ps.setLong(3, nowMillis);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isMissionComplete(UUID uuid, String missionId) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT completed_at FROM missions WHERE uuid = ? AND mission = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, missionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
