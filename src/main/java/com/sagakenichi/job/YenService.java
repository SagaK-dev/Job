package com.sagakenichi.job;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Job's built-in integer currency. It is intentionally independent of Vault. */
final class YenService {
    private final NamespacedKey balanceKey;

    YenService(JavaPlugin plugin) {
        this.balanceKey = new NamespacedKey(plugin, "yen_balance");
    }

    long balance(Player player) {
        Long value = player.getPersistentDataContainer().get(balanceKey, PersistentDataType.LONG);
        return value == null ? 0L : Math.max(0L, value);
    }

    boolean canDeposit(Player player, long amount) {
        if (amount < 0L) return false;
        return balance(player) <= Long.MAX_VALUE - amount;
    }

    boolean deposit(Player player, long amount) {
        if (amount < 0L || !canDeposit(player, amount)) return false;
        set(player, balance(player) + amount);
        return true;
    }

    void set(Player player, long amount) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (amount <= 0L) pdc.remove(balanceKey);
        else pdc.set(balanceKey, PersistentDataType.LONG, amount);
    }

    static String format(long amount) {
        return String.format("%,d円", Math.max(0L, amount));
    }
}
