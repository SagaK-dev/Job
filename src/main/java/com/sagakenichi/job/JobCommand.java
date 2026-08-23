package com.sagakenichi.job;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class JobCommand implements CommandExecutor, TabCompleter {

    private final JobPlugin plugin;
    private final BeginnerService beginners;
    private final RewardService rewards;
    private final RateTable rateTable;

    JobCommand(JobPlugin plugin, BeginnerService beginners, RewardService rewards, RateTable rateTable) {
        this.plugin = plugin;
        this.beginners = beginners;
        this.rewards = rewards;
        this.rateTable = rateTable;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("rates")) {
            if (!require(sender, "job.use")) {
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
            if (!require(sender, "job.use")) {
                return true;
            }
            sendStatus(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("rate") || args[0].equalsIgnoreCase("setrate")) {
            return handleRate(sender, label, args);
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!require(sender, "job.admin")) {
                return true;
            }
            plugin.reloadRuntimeConfig();
            sender.sendMessage("§a[Job] config.yml と単価表を再読み込みしました。");
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    private boolean handleRate(CommandSender sender, String label, String[] args) {
        if (!require(sender, "job.admin")) {
            return true;
        }

        if (args[0].equalsIgnoreCase("setrate")) {
            if (args.length < 3 || args.length > 4) {
                sendRateUsage(sender, label);
                return true;
            }
            String[] normalized = args.length == 3
                    ? new String[]{"rate", "set", args[1], args[2]}
                    : new String[]{"rate", "set", args[1], args[2], args[3]};
            return handleRate(sender, label, normalized);
        }

        if (args.length == 1 || args[1].equalsIgnoreCase("list")) {
            sendRates(sender);
            return true;
        }
        if (args[1].equalsIgnoreCase("get")) {
            if (args.length != 3) {
                sendRateUsage(sender, label);
                return true;
            }
            RewardRule rule = RewardRule.fromInput(args[2]);
            if (rule == null) {
                sendUnknownRule(sender);
                return true;
            }
            sendRate(sender, rule);
            return true;
        }
        if (!args[1].equalsIgnoreCase("set") || (args.length != 4 && args.length != 5)) {
            sendRateUsage(sender, label);
            return true;
        }

        RewardRule rule = RewardRule.fromInput(args[2]);
        if (rule == null) {
            sendUnknownRule(sender);
            return true;
        }

        RateTable.Rate current = rateTable.rate(rule);
        int unitCount;
        int yen;
        if (args.length == 4) {
            unitCount = current.unitCount();
            Integer parsedYen = parsePositiveInt(args[3]);
            if (parsedYen == null || !RateTable.isValidYen(parsedYen)) {
                sender.sendMessage("§c金額は1〜" + RateTable.MAX_YEN_PER_UNIT + "の整数で指定してください。");
                return true;
            }
            yen = parsedYen;
        } else {
            Integer parsedCount = parsePositiveInt(args[3]);
            Integer parsedYen = parsePositiveInt(args[4]);
            if (parsedCount == null || !RateTable.isValidUnitCount(parsedCount)) {
                sender.sendMessage("§c必要個数は1〜" + RateTable.MAX_UNIT_COUNT + "の整数で指定してください。");
                return true;
            }
            if (parsedYen == null || !RateTable.isValidYen(parsedYen)) {
                sender.sendMessage("§c金額は1〜" + RateTable.MAX_YEN_PER_UNIT + "の整数で指定してください。");
                return true;
            }
            unitCount = parsedCount;
            yen = parsedYen;
        }

        rateTable.set(rule, unitCount, yen);
        sender.sendMessage("§a[Job] " + rule.configKey() + " を §f" + unitCount
                + "個=§e" + yen + "円§a に変更し、config.ymlへ保存しました。");
        return true;
    }

    private void sendRates(CommandSender sender) {
        sender.sendMessage("§6--- Job 単価表 ---");
        for (RewardRule rule : RewardRule.values()) {
            sendRate(sender, rule);
        }
        sender.sendMessage("§6初心者（累計プレイ時間が" + beginners.playtimeHours()
                + "時間未満）は上記の支払額×" + beginners.beginnerMultiplier() + "。");
    }

    private void sendRate(CommandSender sender, RewardRule rule) {
        RateTable.Rate rate = rateTable.rate(rule);
        sender.sendMessage("§f" + rule.description() + " §7[" + rule.configKey() + "] §f"
                + rate.unitCount() + "個=§e" + rate.yenPerUnit() + "円");
    }

    private void sendStatus(Player player) {
        if (beginners.isBeginner(player)) {
            long remainingTicks = beginners.remainingTicks(player);
            long remainingMinutes = (remainingTicks + BeginnerService.TICKS_PER_MINUTE - 1L)
                    / BeginnerService.TICKS_PER_MINUTE;
            long hours = remainingMinutes / 60L;
            long minutes = remainingMinutes % 60L;
            long playedTicks = beginners.playedTicks(player);
            long playedMinutesTotal = playedTicks / BeginnerService.TICKS_PER_MINUTE;
            long playedHours = playedMinutesTotal / 60L;
            long playedMinutes = playedMinutesTotal % 60L;
            player.sendMessage("§6初心者ボーナス: ×" + beginners.beginnerMultiplier()
                    + " §f(累計 " + playedHours + "時間" + playedMinutes + "分 / "
                    + beginners.playtimeHours() + "時間、残り " + hours + "時間" + minutes + "分)");
        } else {
            player.sendMessage("§7初心者ボーナス期間は終了しています。倍率: ×1");
        }
        player.sendMessage("§f進捗: 採掘(石系) " + progress(player, RewardRule.MINER_STONE)
                + " | 木こり " + progress(player, RewardRule.LUMBERJACK)
                + " | 農家 " + progress(player, RewardRule.FARMER));
        player.sendMessage("§f進捗: 整地 " + progress(player, RewardRule.LAND_CLEARER)
                + " | 雑草 " + progress(player, RewardRule.WEEDER));
    }

    private String progress(Player player, RewardRule rule) {
        return rewards.progress(player, rule) + "/" + rewards.targetCount(rule);
    }

    private static boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage("§c権限がありません。");
        return false;
    }

    private static Integer parsePositiveInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void sendUnknownRule(CommandSender sender) {
        sender.sendMessage("§c不明な単価キーです。使用可能: " + String.join(", ",
                Arrays.stream(RewardRule.values()).map(RewardRule::configKey).toList()));
    }

    private static void sendRateUsage(CommandSender sender, String label) {
        sender.sendMessage("§e/" + label + " rate list");
        sender.sendMessage("§e/" + label + " rate get <key>");
        sender.sendMessage("§e/" + label + " rate set <key> <yen>");
        sender.sendMessage("§e/" + label + " rate set <key> <unit-count> <yen>");
        sender.sendMessage("§7例: /" + label + " rate set miner-stone 16 2");
    }

    private static void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§e/" + label + " rates §7- 現在の単価表");
        sender.sendMessage("§e/" + label + " status §7- 初心者倍率と進捗");
        if (sender.hasPermission("job.admin")) {
            sender.sendMessage("§e/" + label + " rate <list|get|set> ... §7- 単価変更");
            sender.sendMessage("§e/" + label + " reload §7- 設定再読込");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> roots = new ArrayList<>(List.of("rates", "status"));
            if (sender.hasPermission("job.admin")) {
                roots.add("rate");
                roots.add("setrate");
                roots.add("reload");
            }
            return startsWith(roots, args[0]);
        }
        if (!sender.hasPermission("job.admin")) {
            return List.of();
        }
        if (args[0].equalsIgnoreCase("rate")) {
            if (args.length == 2) {
                return startsWith(List.of("list", "get", "set"), args[1]);
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("get") || args[1].equalsIgnoreCase("set"))) {
                return startsWith(ruleKeys(), args[2]);
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
                return startsWith(List.of("1", "2", "4", "8", "12", "16", "128"), args[3]);
            }
            if (args.length == 5 && args[1].equalsIgnoreCase("set")) {
                return startsWith(List.of("1", "2", "4", "12"), args[4]);
            }
        }
        if (args[0].equalsIgnoreCase("setrate")) {
            if (args.length == 2) {
                return startsWith(ruleKeys(), args[1]);
            }
            if (args.length == 3) {
                return startsWith(List.of("1", "2", "4", "12"), args[2]);
            }
        }
        return List.of();
    }

    private static List<String> ruleKeys() {
        return Arrays.stream(RewardRule.values()).map(RewardRule::configKey).toList();
    }

    private static List<String> startsWith(List<String> values, String rawPrefix) {
        String prefix = rawPrefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
