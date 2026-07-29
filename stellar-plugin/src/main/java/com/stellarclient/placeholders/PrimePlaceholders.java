package com.stellarclient.placeholders;

import com.stellarclient.StellarClientPlugin;
import com.stellarclient.database.Database;
import com.stellarclient.detection.ClientDetectionService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PrimePlaceholders extends PlaceholderExpansion {

    private final StellarClientPlugin plugin;
    private final ClientDetectionService detection;
    private final Database database;

    public PrimePlaceholders(StellarClientPlugin plugin, ClientDetectionService detection, Database database) {
        this.plugin = plugin;
        this.detection = detection;
        this.database = database;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "StellarClient";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Prime";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        return switch (params.toLowerCase()) {
            case "status" -> detection.isVerified(player.getUniqueId()) || player.hasPermission("StellarClient.verified")
                    ? plugin.getConfig().getString("verified.badge", "âš¡")
                    : "";
            case "version" -> detection.getVersion(player.getUniqueId()).orElse(
                    database.findProfile(player.getUniqueId()).map(p -> p.clientVersion()).orElse(""));
            case "level" -> database.findProfile(player.getUniqueId())
                    .map(p -> String.valueOf(p.level()))
                    .orElse("0");
            case "xp" -> database.findProfile(player.getUniqueId())
                    .map(p -> String.valueOf(p.xp()))
                    .orElse("0");
            case "verified" -> detection.isVerified(player.getUniqueId()) ? "true" : "false";
            default -> null;
        };
    }
}
