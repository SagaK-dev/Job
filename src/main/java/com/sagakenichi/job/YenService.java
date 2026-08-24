package com.sagakenichi.job;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Job's yen facade backed by the server's active Vault Economy provider.
 *
 * <p>Job 2.0.x stored a private LONG in the player's PDC. When that legacy value is
 * encountered, it is deposited into Vault once and then removed so existing balances are
 * not silently lost during the transition to the shared server economy.</p>
 */
final class YenService {
    private final JavaPlugin plugin;
    private final Economy economy;
    private final NamespacedKey legacyBalanceKey;

    YenService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.legacyBalanceKey = new NamespacedKey(plugin, "yen_balance");
        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            throw new IllegalStateException("Vault is installed, but no Vault Economy provider is registered.");
        }
        this.economy = registration.getProvider();
    }

    String providerName() {
        return economy.getName();
    }

    long balance(Player player) {
        migrateLegacyBalance(player);
        double value = economy.getBalance(player);
        if (!Double.isFinite(value) || value <= 0.0D) return 0L;
        if (value >= Long.MAX_VALUE) return Long.MAX_VALUE;
        return (long) Math.floor(value + 1.0E-9D);
    }

    boolean canDeposit(Player player, long amount) {
        if (amount < 0L) return false;
        migrateLegacyBalance(player);
        double current = economy.getBalance(player);
        return Double.isFinite(current) && Double.isFinite(current + (double) amount);
    }

    boolean deposit(Player player, long amount) {
        if (amount < 0L) return false;
        migrateLegacyBalance(player);
        if (amount == 0L) return true;
        EconomyResponse response = economy.depositPlayer(player, (double) amount);
        return response != null && response.transactionSuccess();
    }

    void set(Player player, long amount) {
        migrateLegacyBalance(player);
        double target = Math.max(0L, amount);
        double current = economy.getBalance(player);
        if (!Double.isFinite(current)) return;
        double delta = target - current;
        if (Math.abs(delta) < 1.0E-9D) return;
        EconomyResponse response = delta > 0.0D
                ? economy.depositPlayer(player, delta)
                : economy.withdrawPlayer(player, -delta);
        if (response == null || !response.transactionSuccess()) {
            plugin.getLogger().warning("Vault provider '" + providerName()
                    + "' rejected a Job balance adjustment for " + player.getName());
        }
    }

    private void migrateLegacyBalance(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Long legacy = pdc.get(legacyBalanceKey, PersistentDataType.LONG);
        if (legacy == null) return;
        if (legacy <= 0L) {
            pdc.remove(legacyBalanceKey);
            return;
        }

        EconomyResponse response = economy.depositPlayer(player, legacy.doubleValue());
        if (response != null && response.transactionSuccess()) {
            pdc.remove(legacyBalanceKey);
            plugin.getLogger().info("Migrated " + legacy + " yen from Job 2.0.x to Vault for "
                    + player.getName() + " using provider '" + providerName() + "'.");
        } else {
            String error = response == null ? "no response" : response.errorMessage;
            plugin.getLogger().warning("Could not migrate legacy Job yen for " + player.getName()
                    + " to Vault provider '" + providerName() + "': " + error);
        }
    }

    static String format(long amount) {
        return String.format("%,d円", Math.max(0L, amount));
    }
}
