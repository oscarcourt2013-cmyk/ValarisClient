package dev.stellarclient.v26_2.hud;

import dev.stellarclient.core.hud.vanilla.VanillaHudMeasurements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/** Mirrors vanilla scoreboard sidebar sizing for HUD editor bounds. */
public final class ScoreboardSidebarMetrics {

    private static final Comparator<PlayerScoreEntry> SCORE_DISPLAY_ORDER = Comparator
            .comparing(PlayerScoreEntry::value)
            .reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    private ScoreboardSidebarMetrics() {
    }

    public static void update(Minecraft minecraft, int screenWidth, int screenHeight) {
        if (minecraft.level == null || minecraft.player == null) {
            VanillaHudMeasurements.clearScoreboard();
            return;
        }

        Scoreboard scoreboard = minecraft.level.getScoreboard();
        Objective objective = resolveSidebarObjective(minecraft, scoreboard);
        if (objective == null) {
            VanillaHudMeasurements.clearScoreboard();
            return;
        }

        VanillaHudMeasurements.setScoreboard(computeBounds(minecraft.font, objective, screenWidth, screenHeight));
    }

    private static @Nullable Objective resolveSidebarObjective(Minecraft minecraft, Scoreboard scoreboard) {
        Objective objective = null;
        PlayerTeam playerTeam = scoreboard.getPlayersTeam(minecraft.player.getScoreboardName());
        if (playerTeam != null) {
            DisplaySlot displaySlot = playerTeam.getColor().map(TeamColor::displaySlot).orElse(null);
            if (displaySlot != null) {
                objective = scoreboard.getDisplayObjective(displaySlot);
            }
        }
        return objective != null ? objective : scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    private static VanillaHudMeasurements.Bounds computeBounds(
            Font font, Objective objective, int screenWidth, int screenHeight) {
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        int contentWidth = font.width(objective.getDisplayName());
        int colonWidth = font.width(": ");

        List<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(SCORE_DISPLAY_ORDER)
                .limit(15)
                .toList();

        for (PlayerScoreEntry entry : entries) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            Component scoreText = entry.formatValue(numberFormat);
            int scoreWidth = font.width(scoreText);
            contentWidth = Math.max(contentWidth, font.width(name) + (scoreWidth > 0 ? colonWidth + scoreWidth : 0));
        }

        int lineCount = entries.size();
        int width = contentWidth + 4;
        int height = lineCount * 9 + 10;
        int bottom = screenHeight / 2 + lineCount * 9 / 3;
        int top = bottom - lineCount * 9 - 10;
        int left = screenWidth - contentWidth - 5;

        return new VanillaHudMeasurements.Bounds(left, top, width, height);
    }
}
