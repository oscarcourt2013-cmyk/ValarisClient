package dev.stellarclient.v26_2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Reads sidebar scoreboard text for economy modules. */
final class ScoreboardAccess {

    private static final Comparator<PlayerScoreEntry> ORDER = Comparator
            .comparing(PlayerScoreEntry::value)
            .reversed()
            .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);

    private ScoreboardAccess() {
    }

    static String title() {
        Objective objective = sidebarObjective();
        return objective != null ? objective.getDisplayName().getString() : "";
    }

    static int lineCount() {
        return lines().size();
    }

    static String line(int index) {
        List<String> lines = lines();
        return index >= 0 && index < lines.size() ? lines.get(index) : "";
    }

    private static List<String> lines() {
        Objective objective = sidebarObjective();
        if (objective == null) {
            return List.of();
        }
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        List<String> result = new ArrayList<>();
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective).stream()
                .filter(entry -> !entry.isHidden())
                .sorted(ORDER)
                .limit(15)
                .toList()) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            Component scoreText = entry.formatValue(numberFormat);
            String score = scoreText.getString();
            result.add(score.isEmpty() ? name.getString() : name.getString() + ": " + score);
        }
        return result;
    }

    private static @Nullable Objective sidebarObjective() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return null;
        }
        Scoreboard scoreboard = mc.level.getScoreboard();
        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }
}
