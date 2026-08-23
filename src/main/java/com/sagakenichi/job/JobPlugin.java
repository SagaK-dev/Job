package com.sagakenichi.job;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class JobPlugin extends JavaPlugin {
    private Economy economy;
    private BeginnerService beginners;
    private RateTable rateTable;
    private RewardService rewards;
    private PlacedBlockTracker placedBlocks;
    private JobListener listener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!setupEconomy()) {
            getLogger().severe("Vault and a Vault-compatible economy provider are required. Disabling Job.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        beginners = new BeginnerService(
                getConfig().getLong("beginner.duration-hours", 72L),
                getConfig().getInt("beginner.multiplier", 3));
        rateTable = new RateTable(this);
        rewards = new RewardService(this, economy, beginners, rateTable,
                getConfig().getBoolean("messages.payout", true));
        placedBlocks = new PlacedBlockTracker(this,
                getConfig().getBoolean("anti-abuse.ignore-player-placed-blocks", true));
        RewardCatalog catalog = new RewardCatalog();
        listener = new JobListener(catalog, rewards, placedBlocks, beginners,
                getConfig().getBoolean("messages.beginner-join", true));
        getServer().getPluginManager().registerEvents(listener, this);
        PluginCommand command = getCommand("job");
        if (command == null) throw new IllegalStateException("Command 'job' is missing from plugin.yml");
        JobCommand executor = new JobCommand(this, beginners, rewards, rateTable);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getLogger().info("Job 1.1.0 enabled. Beginner rewards are multiplied by "
                + beginners.beginnerMultiplier() + " for the first " + beginners.durationHours()
                + " hours. Rates are configurable with /job rate set.");
    }

    void reloadRuntimeConfig() {
        reloadConfig();
        beginners.reload(getConfig().getLong("beginner.duration-hours", 72L),
                getConfig().getInt("beginner.multiplier", 3));
        rateTable.reload();
        rewards.setPayoutMessages(getConfig().getBoolean("messages.payout", true));
        placedBlocks.setEnabled(getConfig().getBoolean("anti-abuse.ignore-player-placed-blocks", true));
        listener.setBeginnerJoinMessage(getConfig().getBoolean("messages.beginner-join", true));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> registration =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) return false;
        economy = registration.getProvider();
        return economy != null;
    }
}
