package com.sagakenichi.job;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

final class JobListener implements Listener {

    private final RewardCatalog catalog;
    private final RewardService rewards;
    private final PlacedBlockTracker placedBlocks;
    private final BeginnerService beginners;
    private boolean beginnerJoinMessage;

    JobListener(RewardCatalog catalog, RewardService rewards, PlacedBlockTracker placedBlocks,
                BeginnerService beginners, boolean beginnerJoinMessage) {
        this.catalog = catalog;
        this.rewards = rewards;
        this.placedBlocks = placedBlocks;
        this.beginners = beginners;
        this.beginnerJoinMessage = beginnerJoinMessage;
    }

    void setBeginnerJoinMessage(boolean enabled) {
        this.beginnerJoinMessage = enabled;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        placedBlocks.markPlaced(event.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        boolean playerPlaced = placedBlocks.consumeIfPlaced(block);
        RewardRule rule = catalog.classify(block, playerPlaced);
        if (rule != null) {
            rewards.record(event.getPlayer(), rule);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            placedBlocks.consumeIfPlaced(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            placedBlocks.consumeIfPlaced(block);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!beginnerJoinMessage || !beginners.isBeginner(player)) {
            return;
        }
        long remaining = beginners.remainingMillis(player);
        long hours = (remaining + 3_599_999L) / 3_600_000L;
        player.sendMessage("§6[Job] 初回ログインから72時間以内のため、仕事の単価が×"
                + beginners.beginnerMultiplier() + "です。残り約" + hours + "時間。");
    }
}
