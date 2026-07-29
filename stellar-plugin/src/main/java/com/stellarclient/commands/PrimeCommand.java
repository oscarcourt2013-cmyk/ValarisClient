package com.stellarclient.commands;

import com.stellarclient.StellarClientPlugin;
import com.stellarclient.achievements.AchievementService;
import com.stellarclient.profile.ProfileService;
import com.stellarclient.rewards.RewardService;
import com.stellarclient.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PrimeCommand implements CommandExecutor, TabCompleter {

    private final StellarClientPlugin plugin;
    private final ProfileService profiles;
    private final RewardService rewards;
    private final AchievementService achievements;

    public PrimeCommand(
            StellarClientPlugin plugin,
            ProfileService profiles,
            RewardService rewards,
            AchievementService achievements) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.rewards = rewards;
        this.achievements = achievements;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("StellarClient.use")) {
                Text.send(player, "<red>Permission refusÃ©e.</red>");
                return true;
            }
            profiles.openGui(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("StellarClient.reload") && !sender.hasPermission("StellarClient.admin")) {
                    sender.sendMessage("No permission.");
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage("StellarClient reloaded.");
            }
            case "info" -> {
                if (!sender.hasPermission("StellarClient.info") && !sender.hasPermission("StellarClient.admin")) {
                    sender.sendMessage("No permission.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("Usage: /prime info <player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }
                if (sender instanceof Player player) {
                    profiles.sendInfo(player, target);
                } else {
                    sender.sendMessage("Prime=" + plugin.detection().isVerified(target.getUniqueId())
                            + " version=" + plugin.detection().getVersion(target.getUniqueId()).orElse("-"));
                }
            }
            case "rewards" -> {
                if (!(sender instanceof Player player)) {
                    return true;
                }
                boolean claimed = rewards.hasClaimed(player, RewardService.FIRST_JOIN);
                Text.send(player, "<gold>âš¡ Rewards</gold>");
                Text.send(player, "<gray>first_join:</gray> " + (claimed ? "<green>claimed</green>" : "<yellow>available after Prime handshake</yellow>"));
            }
            case "achievements" -> {
                if (!(sender instanceof Player player)) {
                    return true;
                }
                Text.send(player, "<gold>âš¡ Achievements</gold>");
                var unlocked = achievements.unlocked(player);
                for (var def : achievements.definitions().values()) {
                    boolean ok = unlocked.contains(def.id());
                    Text.send(player, (ok ? "<green>âœ”</green> " : "<dark_gray>â˜</dark_gray> ")
                            + "<white>" + def.name() + "</white> <gray>â€” " + def.description() + "</gray>");
                }
            }
            default -> sender.sendMessage("Usage: /prime [reload|info|rewards|achievements]");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("reload", "info", "rewards", "achievements")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
