package com.sagakenichi.job;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;

final class RewardService {
    private final JavaPlugin plugin;
    private final YenService yen;
    private final BeginnerService beginners;
    private final RateTable rateTable;
    private final Map<RewardRule, NamespacedKey> progressKeys = new EnumMap<>(RewardRule.class);
    private boolean payoutMessages;

    RewardService(JavaPlugin plugin, YenService yen, BeginnerService beginners,
                  RateTable rateTable, boolean payoutMessages) {
        this.plugin = plugin;
        this.yen = yen;
        this.beginners = beginners;
        this.rateTable = rateTable;
        this.payoutMessages = payoutMessages;
        for (RewardRule rule : RewardRule.values()) progressKeys.put(rule, new NamespacedKey(plugin, rule.progressKey()));
    }

    void setPayoutMessages(boolean payoutMessages) { this.payoutMessages = payoutMessages; }

    void record(Player player, RewardRule rule) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = progressKeys.get(rule);
        RateTable.Rate rate = rateTable.rate(rule);
        int previous = Math.max(0, pdc.getOrDefault(key, PersistentDataType.INTEGER, 0));
        int progress = previous == Integer.MAX_VALUE ? previous : previous + 1;
        int completedUnits = progress / rate.unitCount();
        if (completedUnits <= 0) {
            pdc.set(key, PersistentDataType.INTEGER, progress);
            return;
        }
        int multiplier = beginners.multiplier(player);
        long payout;
        try {
            payout = Math.multiplyExact(Math.multiplyExact((long) completedUnits, rate.yenPerUnit()), multiplier);
        } catch (ArithmeticException ex) {
            plugin.getLogger().warning("Refusing overflowing yen payout for " + player.getName());
            pdc.set(key, PersistentDataType.INTEGER, progress);
            return;
        }
        if (payout <= 0L || !yen.deposit(player, payout)) {
            plugin.getLogger().warning("Could not add Job yen for " + player.getName() + ": " + payout);
            pdc.set(key, PersistentDataType.INTEGER, progress);
            return;
        }
        int remainder = progress % rate.unitCount();
        if (remainder == 0) pdc.remove(key); else pdc.set(key, PersistentDataType.INTEGER, remainder);
        if (payoutMessages) {
            String beginner = multiplier > 1 ? " §6[初心者×" + multiplier + "]" : "";
            player.sendMessage("§a[Job] §f" + rule.displayName() + "報酬: §e" + YenService.format(payout) + beginner);
        }
    }

    int progress(Player player, RewardRule rule) {
        return Math.max(0, player.getPersistentDataContainer().getOrDefault(progressKeys.get(rule), PersistentDataType.INTEGER, 0));
    }
    int targetCount(RewardRule rule) { return rateTable.rate(rule).unitCount(); }
}
