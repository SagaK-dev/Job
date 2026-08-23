package com.sagakenichi.job;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
        Block block = event.getBlockPlaced();
        if (catalog.requiresPlacedTracking(block.getType())) {
            placedBlocks.markPlaced(block);
        } else {
            placedBlocks.consumeIfPlaced(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        boolean playerPlaced = placedBlocks.consumeIfPlaced(block);
        if (!earnsRewards(event.getPlayer())) {
            return;
        }
        RewardRule rule = catalog.classify(block, playerPlaced);
        if (rule != null) {
            rewards.record(event.getPlayer(), rule);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRightClickHarvest(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || !earnsRewards(event.getPlayer())) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        ItemStack item = event.getItem();
        Material held = item == null ? Material.AIR : item.getType();
        if (catalog.isRightClickBerryHarvest(block, held)) {
            rewards.record(event.getPlayer(), RewardRule.FARMER);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        placedBlocks.moveMarkedBlocks(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        placedBlocks.moveMarkedBlocks(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlockChange(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock fallingBlock)) {
            return;
        }
        placedBlocks.moveToFallingEntity(event.getBlock(), fallingBlock, event.getTo());
        placedBlocks.restoreFromFallingEntity(fallingBlock, event.getBlock(), event.getTo());
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
        long remainingTicks = beginners.remainingTicks(player);
        long remainingHours = (remainingTicks + BeginnerService.TICKS_PER_HOUR - 1L)
                / BeginnerService.TICKS_PER_HOUR;
        player.sendMessage("§6[Job] 累計プレイ時間が" + beginners.playtimeHours()
                + "時間未満のため、仕事の単価が×" + beginners.beginnerMultiplier()
                + "です。残りプレイ時間は約" + remainingHours + "時間です。");
    }

    private static boolean earnsRewards(Player player) {
        GameMode gameMode = player.getGameMode();
        return gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR;
    }
}
