package com.valarisclient.database;

import com.valarisclient.ValarisClientPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseFactory {

    private DatabaseFactory() {
    }

    public static Database create(ValarisClientPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        String type = cfg.getString("database.type", "sqlite");
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("ValarisClient");
        hikari.setMaximumPoolSize(cfg.getInt("database.mysql.pool-size", 8));
        hikari.setConnectionTimeout(10_000);

        if ("mysql".equalsIgnoreCase(type)) {
            String host = cfg.getString("database.mysql.host", "127.0.0.1");
            int port = cfg.getInt("database.mysql.port", 3306);
            String db = cfg.getString("database.mysql.database", "ValarisClient");
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            hikari.setUsername(cfg.getString("database.mysql.username", "valaris"));
            hikari.setPassword(cfg.getString("database.mysql.password", ""));
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File file = new File(plugin.getDataFolder(), cfg.getString("database.sqlite-file", "ValarisClient.db"));
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdirs();
            hikari.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setMaximumPoolSize(4);
        }

        HikariDataSource ds = new HikariDataSource(hikari);
        return new SqlDatabase(ds, !"mysql".equalsIgnoreCase(type));
    }

    static void createSchema(Connection connection, boolean sqlite) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                      uuid VARCHAR(36) PRIMARY KEY,
                      username VARCHAR(16) NOT NULL,
                      valaris_client INTEGER NOT NULL DEFAULT 0,
                      client_version VARCHAR(32) DEFAULT '',
                      first_join BIGINT NOT NULL DEFAULT 0,
                      playtime_seconds BIGINT NOT NULL DEFAULT 0,
                      xp INTEGER NOT NULL DEFAULT 0,
                      level INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS achievements (
                      uuid VARCHAR(36) NOT NULL,
                      achievement VARCHAR(64) NOT NULL,
                      unlocked_at BIGINT NOT NULL,
                      PRIMARY KEY (uuid, achievement)
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS rewards (
                      uuid VARCHAR(36) NOT NULL,
                      reward VARCHAR(64) NOT NULL,
                      claimed_at BIGINT NOT NULL,
                      PRIMARY KEY (uuid, reward)
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS missions (
                      uuid VARCHAR(36) NOT NULL,
                      mission VARCHAR(64) NOT NULL,
                      progress INTEGER NOT NULL DEFAULT 0,
                      completed_at BIGINT NOT NULL DEFAULT 0,
                      PRIMARY KEY (uuid, mission)
                    )
                    """);
        }
    }
}
