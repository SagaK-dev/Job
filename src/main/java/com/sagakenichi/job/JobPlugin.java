package com.sagakenichi.job;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class JobPlugin extends JavaPlugin {
    private static final long DEFAULT_BEGINNER_PLAYTIME_HOURS = 24L;
    private BeginnerService beginners;
    private RateTable rateTable;
    private RewardService rewards;
    private PlacedBlockTracker placedBlocks;
    private JobListener listener;
    private YenService yen;
    private JobSellMenu sellMenu;
    private JobVaultEconomy vaultEconomy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateBeginnerConfig();
        beginners = new BeginnerService(
                getConfig().getLong("beginner.playtime-hours", DEFAULT_BEGINNER_PLAYTIME_HOURS),
                getConfig().getInt("beginner.multiplier", 3));
        rateTable = new RateTable(this);
        yen = new YenService(this);
        rewards = new RewardService(this, yen, beginners, rateTable,
                getConfig().getBoolean("messages.payout", true));
        placedBlocks = new PlacedBlockTracker(this,
                getConfig().getBoolean("anti-abuse.ignore-player-placed-blocks", true));
        listener = new JobListener(new RewardCatalog(), rewards, placedBlocks, beginners,
                getConfig().getBoolean("messages.beginner-join", true));
        sellMenu = new JobSellMenu(yen, beginners, rateTable, new SellCatalog());

        getServer().getPluginManager().registerEvents(yen, this);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(sellMenu, this);
        yen.start();
        registerVaultBridgeIfAvailable();

        JobCommand executor = new JobCommand(this, beginners, rewards, rateTable, yen, sellMenu);
        registerCommand("job", executor);
        registerCommand("jobmenu", executor);

        getLogger().info("Job 2.0.2 enabled. Existing Job rewards and sell GUI use the same yen balance; Vault-backed plugins can use that balance when Vault is installed.");
    }

    @Override
    public void onDisable() {
        if (vaultEconomy != null) {
            getServer().getServicesManager().unregister(Economy.class, vaultEconomy);
            vaultEconomy = null;
        }
        if (yen != null) yen.shutdown();
    }

    boolean vaultBridgeActive() {
        return vaultEconomy != null;
    }

    private void registerVaultBridgeIfAvailable() {
        if (!getServer().getPluginManager().isPluginEnabled("Vault")) {
            getLogger().info("Vault is not installed; Job yen remains fully usable internally and the Vault bridge is skipped.");
            return;
        }
        vaultEconomy = new JobVaultEconomy(this, yen);
        getServer().getServicesManager().register(Economy.class, vaultEconomy, this, ServicePriority.Highest);
        getLogger().info("Registered JobYen as the Vault economy provider. BdayoLand transactions now use the same Job yen balance.");
    }

    private void registerCommand(String name, JobCommand executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '" + name + "' is missing from plugin.yml");
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    void reloadRuntimeConfig() {
        reloadConfig();
        migrateBeginnerConfig();
        beginners.reload(
                getConfig().getLong("beginner.playtime-hours", DEFAULT_BEGINNER_PLAYTIME_HOURS),
                getConfig().getInt("beginner.multiplier", 3));
        rateTable.reload();
        rewards.setPayoutMessages(getConfig().getBoolean("messages.payout", true));
        placedBlocks.setEnabled(getConfig().getBoolean("anti-abuse.ignore-player-placed-blocks", true));
        listener.setBeginnerJoinMessage(getConfig().getBoolean("messages.beginner-join", true));
    }

    private void migrateBeginnerConfig() {
        boolean changed = false;
        if (!getConfig().contains("beginner.playtime-hours")) {
            getConfig().set("beginner.playtime-hours", DEFAULT_BEGINNER_PLAYTIME_HOURS);
            changed = true;
        }
        if (getConfig().contains("beginner.duration-hours")) {
            getConfig().set("beginner.duration-hours", null);
            changed = true;
        }
        if (changed) {
            saveConfig();
        }
    }
}
