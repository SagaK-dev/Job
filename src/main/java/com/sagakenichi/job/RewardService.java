package com.sagakenichi.job;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;

final class RewardService {

    private final JavaPlugin plugin;
    private final Economy economy;
    private final BeginnerService beginners;
    private final Map<RewardRule, NamespacedKey> progressKeys = new EnumMap<>(RewardRule.class);
    private boolean payoutMessages;

    RewardService(JavaPlugin plugin, Economy economy, BeginnerService beginners, boolean payoutMessages) {
        this.plugin = plugin;
        this.economy = economy;
        this.beginners = beginners;
        this.payoutMessages = payoutMessages;
        for (RewardRule rule : RewardRule.values()) {
            progressKeys.put(rule, new NamespacedKey(plugin, rule.progressKey()));
        }
    }

    void setPayoutMessages(boolean payoutMessages) {
        this.payoutMessages = payoutMessages;
    }

    void record(Player player, RewardRule rule) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = progressKeys.get(rule);
        int progress = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0) + 1;
        int completedUnits = progress / rule.unitCount();

        if (completedUnits <= 0) {
            pdc.set(key, PersistentDataType.INTEGER, progress);
            return;
        }

        int multiplier = beginners.multiplier(player);
        int yen = completedUnits * rule.yenPerUnit() * multiplier;
        EconomyResponse response = economy.depositPlayer(player, yen);
        if (!response.transactionSuccess()) {
            pdc.set(key, PersistentDataType.INTEGER, progress);
            plugin.getLogger().warning("Could not pay " + player.getName() + ": " + response.errorMessage);
            return;
        }

        int remainder = progress % rule.unitCount();
        if (remainder == 0) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.INTEGER, remainder);
        }

        if (payoutMessages) {
            String beginner = multiplier > 1 ? " §6[初心者×" + multiplier + "]" : "";
            player.sendMessage("§a[Job] §f" + rule.displayName() + "報酬: §e" + yen + "円" + beginner);
        }
    }

    int progress(Player player, RewardRule rule) {
        return player.getPersistentDataContainer().getOrDefault(
                progressKeys.get(rule), PersistentDataType.INTEGER, 0);
    }
}
