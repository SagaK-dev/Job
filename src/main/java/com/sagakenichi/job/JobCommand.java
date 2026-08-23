package com.sagakenichi.job;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class JobCommand implements CommandExecutor, TabCompleter {

    private final JobPlugin plugin;
    private final BeginnerService beginners;
    private final RewardService rewards;

    JobCommand(JobPlugin plugin, BeginnerService beginners, RewardService rewards) {
        this.plugin = plugin;
        this.beginners = beginners;
        this.rewards = rewards;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("rates")) {
            if (!sender.hasPermission("job.use")) {
                sender.sendMessage("§c権限がありません。");
                return true;
            }
            sendRates(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
                return true;
            }
            if (!sender.hasPermission("job.use")) {
                sender.sendMessage("§c権限がありません。");
                return true;
            }
            sendStatus(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("job.admin")) {
                sender.sendMessage("§c権限がありません。");
                return true;
            }
            plugin.reloadRuntimeConfig();
            sender.sendMessage("§a[Job] config.yml を再読み込みしました。");
            return true;
        }

        sender.sendMessage("§e/" + label + " rates §7- 単価表");
        sender.sendMessage("§e/" + label + " status §7- 初心者倍率と進捗");
        if (sender.hasPermission("job.admin")) {
            sender.sendMessage("§e/" + label + " reload §7- 設定再読込");
        }
        return true;
    }

    private void sendRates(CommandSender sender) {
        sender.sendMessage("§6--- Job 単価表 ---");
        sender.sendMessage("§f採掘師: 石/丸石/深層岩など 16個=1円");
        sender.sendMessage("§f採掘師: 石炭/銅/鉄鉱石 1個=2円（深層岩鉱石も同額）");
        sender.sendMessage("§f採掘師: 金/レッドストーン/ラピス鉱石 1個=4円");
        sender.sendMessage("§f採掘師: ダイヤ/エメラルド鉱石 1個=12円");
        sender.sendMessage("§f木こり: 原木・木・幹・菌糸 8個=1円");
        sender.sendMessage("§f農家: 成熟作物の収穫 8個=1円");
        sender.sendMessage("§f整地屋: dirt/sand/gravel/clayなど 16個=1円");
        sender.sendMessage("§f雑草抜き: grass/fern/tall_grass/dead_bushなど 128個=1円");
        sender.sendMessage("§6初心者（初回ログインから72時間以内）は上記単価×"
                + beginners.beginnerMultiplier() + "。");
    }

    private void sendStatus(Player player) {
        if (beginners.isBeginner(player)) {
            long remaining = beginners.remainingMillis(player);
            long hours = remaining / 3_600_000L;
            long minutes = (remaining % 3_600_000L) / 60_000L;
            player.sendMessage("§6初心者ボーナス: ×" + beginners.beginnerMultiplier()
                    + " §f(残り " + hours + "時間" + minutes + "分)");
        } else {
            player.sendMessage("§7初心者ボーナス期間は終了しています。倍率: ×1");
        }
        player.sendMessage("§f進捗: 採掘(石系) " + rewards.progress(player, RewardRule.MINER_STONE) + "/16"
                + " | 木こり " + rewards.progress(player, RewardRule.LUMBERJACK) + "/8"
                + " | 農家 " + rewards.progress(player, RewardRule.FARMER) + "/8");
        player.sendMessage("§f進捗: 整地 " + rewards.progress(player, RewardRule.LAND_CLEARER) + "/16"
                + " | 雑草 " + rewards.progress(player, RewardRule.WEEDER) + "/128");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return List.of("rates", "status", "reload").stream()
                .filter(value -> !value.equals("reload") || sender.hasPermission("job.admin"))
                .filter(value -> value.startsWith(prefix))
                .toList();
    }
}
