package com.sagakenichi.job;

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

/** Vault economy adapter backed by the exact same Job yen balance. */
final class JobVaultEconomy extends AbstractEconomy {
    private static final String NAME = "JobYen";
    private static final String NO_BANKS = "Job yen does not support bank accounts.";

    private final JobPlugin plugin;
    private final YenService yen;

    JobVaultEconomy(JobPlugin plugin, YenService yen) {
        this.plugin = plugin;
        this.yen = yen;
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return YenAmount.FRACTIONAL_DIGITS;
    }

    @Override
    public String format(double amount) {
        return YenService.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return "円";
    }

    @Override
    public String currencyNameSingular() {
        return "円";
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null && yen.hasAccount(player);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAccount(String playerName) {
        return playerName != null && hasAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return player == null ? 0.0D : yen.balanceExact(player);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public double getBalance(String playerName) {
        return playerName == null ? 0.0D : getBalance(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return player != null && yen.has(player, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean has(String playerName, double amount) {
        return playerName != null && has(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null) return failure(amount, 0.0D, "Player is required.");
        if (!validAmount(amount)) return failure(amount, getBalance(player), "Amount must be finite and non-negative.");
        if (!yen.withdraw(player, amount)) return failure(amount, getBalance(player), "残高が不足しています。");
        double rounded = YenAmount.minorToDecimalYen(YenAmount.decimalYenToMinor(amount));
        return success(rounded, getBalance(player));
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return playerName == null ? failure(amount, 0.0D, "Player is required.")
                : withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null) return failure(amount, 0.0D, "Player is required.");
        if (!validAmount(amount)) return failure(amount, getBalance(player), "Amount must be finite and non-negative.");
        if (!yen.deposit(player, amount)) return failure(amount, getBalance(player), "円残高の上限を超えます。");
        double rounded = YenAmount.minorToDecimalYen(YenAmount.decimalYenToMinor(amount));
        return success(rounded, getBalance(player));
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return playerName == null ? failure(amount, 0.0D, "Player is required.")
                : depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return player != null && yen.createAccount(player);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean createPlayerAccount(String playerName) {
        return playerName != null && createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return notImplemented();
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return notImplemented();
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    private static boolean validAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0D) return false;
        try {
            YenAmount.decimalYenToMinor(amount);
            return true;
        } catch (IllegalArgumentException | ArithmeticException ex) {
            return false;
        }
    }

    private static EconomyResponse success(double amount, double balance) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, "");
    }

    private static EconomyResponse failure(double amount, double balance, String error) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, error);
    }

    private static EconomyResponse notImplemented() {
        return new EconomyResponse(0.0D, 0.0D, EconomyResponse.ResponseType.NOT_IMPLEMENTED, NO_BANKS);
    }
}
