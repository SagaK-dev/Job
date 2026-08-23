package com.sagakenichi.job;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

import java.util.EnumSet;
import java.util.Set;

final class RewardCatalog {
    private static final Set<Material> MINER_STONE = EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
            Material.GRANITE, Material.DIORITE, Material.ANDESITE, Material.TUFF, Material.CALCITE,
            Material.BLACKSTONE, Material.BASALT, Material.SMOOTH_BASALT);
    private static final Set<Material> LOW_ORE = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.COPPER_ORE,
            Material.DEEPSLATE_COPPER_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE);
    private static final Set<Material> MID_ORE = EnumSet.of(
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE);
    private static final Set<Material> HIGH_ORE = EnumSet.of(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);
    private static final Set<Material> LAND_CLEARING = EnumSet.of(
            Material.DIRT, Material.COARSE_DIRT, Material.ROOTED_DIRT, Material.GRASS_BLOCK,
            Material.PODZOL, Material.MYCELIUM, Material.MUD, Material.MUDDY_MANGROVE_ROOTS,
            Material.SAND, Material.RED_SAND, Material.GRAVEL, Material.CLAY);
    private static final Set<Material> WEEDS = EnumSet.of(
            Material.SHORT_GRASS, Material.FERN, Material.LARGE_FERN, Material.TALL_GRASS, Material.DEAD_BUSH);
    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART,
            Material.COCOA, Material.SWEET_BERRY_BUSH, Material.PITCHER_CROP, Material.TORCHFLOWER_CROP,
            Material.PUMPKIN, Material.MELON, Material.TORCHFLOWER, Material.PITCHER_PLANT);
    private static final Set<Material> GENERATED_HARVEST_BLOCKS = EnumSet.of(
            Material.PUMPKIN, Material.MELON, Material.TORCHFLOWER, Material.PITCHER_PLANT);

    RewardRule classify(Block block, boolean playerPlaced) {
        Material material = block.getType();
        if (LOW_ORE.contains(material)) return playerPlaced ? null : RewardRule.MINER_LOW_ORE;
        if (MID_ORE.contains(material)) return playerPlaced ? null : RewardRule.MINER_MID_ORE;
        if (HIGH_ORE.contains(material)) return playerPlaced ? null : RewardRule.MINER_HIGH_ORE;
        if (MINER_STONE.contains(material)) return playerPlaced ? null : RewardRule.MINER_STONE;
        if (isLogOrStem(material)) return playerPlaced ? null : RewardRule.LUMBERJACK;
        if (CROPS.contains(material) && isHarvestReady(block, playerPlaced)) return RewardRule.FARMER;
        if (LAND_CLEARING.contains(material)) return playerPlaced ? null : RewardRule.LAND_CLEARER;
        if (WEEDS.contains(material)) return playerPlaced ? null : RewardRule.WEEDER;
        return null;
    }

    boolean isRightClickBerryHarvest(Block block, Material heldMaterial) {
        if (block.getType() != Material.SWEET_BERRY_BUSH || heldMaterial == Material.BONE_MEAL) return false;
        if (!(block.getBlockData() instanceof Ageable ageable)) return false;
        return ageable.getAge() >= 2;
    }

    boolean requiresPlacedTracking(Material material) {
        return LOW_ORE.contains(material) || MID_ORE.contains(material) || HIGH_ORE.contains(material)
                || MINER_STONE.contains(material) || isLogOrStem(material) || LAND_CLEARING.contains(material)
                || WEEDS.contains(material) || GENERATED_HARVEST_BLOCKS.contains(material);
    }

    private static boolean isLogOrStem(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD")
                || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }
    private static boolean isHarvestReady(Block block, boolean playerPlaced) {
        if (block.getBlockData() instanceof Ageable ageable) return ageable.getAge() >= ageable.getMaximumAge();
        return !playerPlaced && GENERATED_HARVEST_BLOCKS.contains(block.getType());
    }
}
