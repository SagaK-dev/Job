package com.sagakenichi.job;

import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Job's built-in yen currency.
 *
 * <p>Existing whole-yen PDC balances remain compatible. A small file-backed mirror makes
 * the same balance available for OfflinePlayer transactions such as BdayoLand tax/rent,
 * while two fixed decimal places preserve BdayoLand's configured fractional prices.</p>
 */
final class YenService implements Listener {
    private static final String ACCOUNTS = "accounts";
    private static final long SAVE_INTERVAL_TICKS = 100L;

    private final JavaPlugin plugin;
    private final NamespacedKey legacyBalanceKey;
    private final File storageFile;
    private final YamlConfiguration storage;
    private final Map<UUID, Long> balancesMinor = new HashMap<>();
    private BukkitTask saveTask;
    private boolean dirty;

    YenService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.legacyBalanceKey = new NamespacedKey(plugin, "yen_balance");
        this.storageFile = new File(plugin.getDataFolder(), "yen-balances.yml");
        this.storage = YamlConfiguration.loadConfiguration(storageFile);
        loadAccounts();
    }

    void start() {
        for (Player player : plugin.getServer().getOnlinePlayers()) migrateAndSync(player);
        saveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::flush, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) migrateAndSync(player);
        flush();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        migrateAndSync(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        migrateAndSync(event.getPlayer());
        flush();
    }

    long balance(Player player) {
        return YenAmount.minorToWholeYen(balanceMinor(player));
    }

    String formattedBalance(Player player) {
        return YenAmount.formatMinor(balanceMinor(player));
    }

    double balanceExact(OfflinePlayer player) {
        return YenAmount.minorToDecimalYen(balanceMinor(player));
    }

    boolean has(OfflinePlayer player, double amount) {
        Long minor = tryDecimalToMinor(amount);
        return minor != null && balanceMinor(player) >= minor;
    }

    boolean canDeposit(Player player, long amount) {
        if (amount < 0L) return false;
        try {
            long add = YenAmount.wholeYenToMinor(amount);
            return balanceMinor(player) <= Long.MAX_VALUE - add;
        } catch (ArithmeticException | IllegalArgumentException ex) {
            return false;
        }
    }

    boolean deposit(Player player, long amount) {
        if (!canDeposit(player, amount)) return false;
        try {
            return addMinor(player, YenAmount.wholeYenToMinor(amount));
        } catch (ArithmeticException | IllegalArgumentException ex) {
            return false;
        }
    }

    boolean deposit(OfflinePlayer player, double amount) {
        Long minor = tryDecimalToMinor(amount);
        return minor != null && addMinor(player, minor);
    }

    boolean withdraw(OfflinePlayer player, double amount) {
        Long minor = tryDecimalToMinor(amount);
        if (minor == null) return false;
        long current = balanceMinor(player);
        if (current < minor) return false;
        setMinor(player, current - minor);
        return true;
    }

    void set(Player player, long amount) {
        long safe = Math.max(0L, amount);
        try {
            setMinor(player, YenAmount.wholeYenToMinor(safe));
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("yen balance is too large", ex);
        }
    }

    boolean createAccount(OfflinePlayer player) {
        UUID id = player.getUniqueId();
        if (!balancesMinor.containsKey(id)) {
            balancesMinor.put(id, 0L);
            writeAccount(id, 0L, true);
        }
        if (player instanceof Player online) syncLegacyPdc(online, balancesMinor.get(id));
        return true;
    }

    boolean hasAccount(OfflinePlayer player) {
        if (player instanceof Player online) migrateAndSync(online);
        return balancesMinor.containsKey(player.getUniqueId()) || player.hasPlayedBefore() || player.isOnline();
    }

    private long balanceMinor(OfflinePlayer player) {
        if (player instanceof Player online) migrateAndSync(online);
        return Math.max(0L, balancesMinor.getOrDefault(player.getUniqueId(), 0L));
    }

    private boolean addMinor(OfflinePlayer player, long amountMinor) {
        if (amountMinor < 0L) return false;
        long current = balanceMinor(player);
        if (current > Long.MAX_VALUE - amountMinor) return false;
        setMinor(player, current + amountMinor);
        return true;
    }

    private void setMinor(OfflinePlayer player, long amountMinor) {
        UUID id = player.getUniqueId();
        long safe = Math.max(0L, amountMinor);
        balancesMinor.put(id, safe);
        writeAccount(id, safe, true);
        if (player instanceof Player online) syncLegacyPdc(online, safe);
        if (!player.isOnline()) flush();
    }

    private void migrateAndSync(Player player) {
        UUID id = player.getUniqueId();
        String base = accountPath(id);
        boolean migrated = storage.getBoolean(base + ".migrated", false);
        long storedMinor = Math.max(0L, balancesMinor.getOrDefault(id, 0L));

        if (!migrated) {
            Long legacyYen = player.getPersistentDataContainer().get(legacyBalanceKey, PersistentDataType.LONG);
            if (legacyYen != null && legacyYen > 0L) {
                try {
                    long legacyMinor = YenAmount.wholeYenToMinor(legacyYen);
                    storedMinor = storedMinor > Long.MAX_VALUE - legacyMinor ? Long.MAX_VALUE : storedMinor + legacyMinor;
                } catch (ArithmeticException ex) {
                    storedMinor = Long.MAX_VALUE;
                }
            }
            balancesMinor.put(id, storedMinor);
            writeAccount(id, storedMinor, true);
        }
        syncLegacyPdc(player, storedMinor);
    }

    private void syncLegacyPdc(Player player, long minor) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        long whole = YenAmount.minorToWholeYen(minor);
        if (whole <= 0L) pdc.remove(legacyBalanceKey);
        else pdc.set(legacyBalanceKey, PersistentDataType.LONG, whole);
    }

    private void loadAccounts() {
        ConfigurationSection section = storage.getConfigurationSection(ACCOUNTS);
        if (section == null) return;
        for (String raw : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(raw);
                long minor = Math.max(0L, section.getLong(raw + ".minor", 0L));
                balancesMinor.put(id, minor);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Ignoring invalid UUID in yen-balances.yml: " + raw);
            }
        }
    }

    private void writeAccount(UUID id, long minor, boolean migrated) {
        String base = accountPath(id);
        storage.set(base + ".minor", Math.max(0L, minor));
        storage.set(base + ".migrated", migrated);
        dirty = true;
    }

    private String accountPath(UUID id) {
        return ACCOUNTS + "." + id;
    }

    private void flush() {
        if (!dirty) return;
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create Job data directory for yen balances.");
                return;
            }
            storage.save(storageFile);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save yen-balances.yml: " + ex.getMessage());
        }
    }

    private static Long tryDecimalToMinor(double amount) {
        try {
            return YenAmount.decimalYenToMinor(amount);
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return null;
        }
    }

    static String format(long amount) {
        return String.format("%,d円", Math.max(0L, amount));
    }

    static String format(double amount) {
        try {
            return YenAmount.formatDecimal(amount);
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return "0円";
        }
    }
}
