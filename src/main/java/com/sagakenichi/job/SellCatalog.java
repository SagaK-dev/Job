package com.sagakenichi.job;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Fixed item-to-job-category mapping. Prices come from the live RateTable. */
final class SellCatalog {
    enum Category {
        MINING("採掘・鉱石", Material.IRON_PICKAXE),
        WOOD("木材", Material.OAK_LOG),
        FARM("農作物", Material.WHEAT),
        TERRAIN("整地素材", Material.GRASS_BLOCK),
        PLANTS("草・植物", Material.FERN);

        private final String displayName;
        private final Material icon;

        Category(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        String displayName() { return displayName; }
        Material icon() { return icon; }
    }

    record Entry(Category category, Material material, String displayName, RewardRule rule) {}

    private final Map<Category, List<Entry>> entries = new EnumMap<>(Category.class);

    SellCatalog() {
        entries.put(Category.MINING, List.of(
                e(Category.MINING, Material.STONE, "石", RewardRule.MINER_STONE),
                e(Category.MINING, Material.COBBLESTONE, "丸石", RewardRule.MINER_STONE),
                e(Category.MINING, Material.ANDESITE, "安山岩", RewardRule.MINER_STONE),
                e(Category.MINING, Material.DIORITE, "閃緑岩", RewardRule.MINER_STONE),
                e(Category.MINING, Material.GRANITE, "花崗岩", RewardRule.MINER_STONE),
                e(Category.MINING, Material.DEEPSLATE, "深層岩", RewardRule.MINER_STONE),
                e(Category.MINING, Material.COBBLED_DEEPSLATE, "深層岩の丸石", RewardRule.MINER_STONE),
                e(Category.MINING, Material.TUFF, "凝灰岩", RewardRule.MINER_STONE),
                e(Category.MINING, Material.COAL, "石炭", RewardRule.MINER_LOW_ORE),
                e(Category.MINING, Material.RAW_COPPER, "銅の原石", RewardRule.MINER_LOW_ORE),
                e(Category.MINING, Material.RAW_IRON, "鉄の原石", RewardRule.MINER_LOW_ORE),
                e(Category.MINING, Material.RAW_GOLD, "金の原石", RewardRule.MINER_MID_ORE),
                e(Category.MINING, Material.REDSTONE, "レッドストーン", RewardRule.MINER_MID_ORE),
                e(Category.MINING, Material.LAPIS_LAZULI, "ラピスラズリ", RewardRule.MINER_MID_ORE),
                e(Category.MINING, Material.DIAMOND, "ダイヤモンド", RewardRule.MINER_HIGH_ORE),
                e(Category.MINING, Material.EMERALD, "エメラルド", RewardRule.MINER_HIGH_ORE)
        ));
        entries.put(Category.WOOD, List.of(
                e(Category.WOOD, Material.OAK_LOG, "オークの原木", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.SPRUCE_LOG, "トウヒの原木", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.BIRCH_LOG, "シラカバの原木", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.JUNGLE_LOG, "ジャングルの原木", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.ACACIA_LOG, "アカシアの原木", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.DARK_OAK_LOG, "ダークオークの原木", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.MANGROVE_LOG, "マングローブの原木", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.CHERRY_LOG, "サクラの原木", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.CRIMSON_STEM, "真紅の幹", RewardRule.LUMBERJACK),
                e(Category.WOOD, Material.WARPED_STEM, "歪んだ幹", RewardRule.LUMBERJACK)
        ));
        entries.put(Category.FARM, List.of(
                e(Category.FARM, Material.WHEAT, "小麦", RewardRule.FARMER),
                e(Category.FARM, Material.CARROT, "ニンジン", RewardRule.FARMER),
                e(Category.FARM, Material.POTATO, "ジャガイモ", RewardRule.FARMER),
                e(Category.FARM, Material.BEETROOT, "ビートルート", RewardRule.FARMER),
                e(Category.FARM, Material.MELON_SLICE, "スイカの薄切り", RewardRule.FARMER),
                e(Category.FARM, Material.PUMPKIN, "カボチャ", RewardRule.FARMER),
                e(Category.FARM, Material.SWEET_BERRIES, "スイートベリー", RewardRule.FARMER),
                e(Category.FARM, Material.GLOW_BERRIES, "グロウベリー", RewardRule.FARMER)
        ));
        entries.put(Category.TERRAIN, List.of(
                e(Category.TERRAIN, Material.DIRT, "土", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.COARSE_DIRT, "粗い土", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.ROOTED_DIRT, "根付いた土", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.GRASS_BLOCK, "草ブロック", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.PODZOL, "ポドゾル", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.MYCELIUM, "菌糸", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.SAND, "砂", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.RED_SAND, "赤い砂", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.GRAVEL, "砂利", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.CLAY, "粘土", RewardRule.LAND_CLEARER),
                e(Category.TERRAIN, Material.MUD, "泥", RewardRule.LAND_CLEARER)
        ));
        entries.put(Category.PLANTS, List.of(
                e(Category.PLANTS, Material.SHORT_GRASS, "草", RewardRule.WEEDER),
                e(Category.PLANTS, Material.FERN, "シダ", RewardRule.WEEDER),
                e(Category.PLANTS, Material.LARGE_FERN, "大きなシダ", RewardRule.WEEDER),
                e(Category.PLANTS, Material.TALL_GRASS, "背の高い草", RewardRule.WEEDER),
                e(Category.PLANTS, Material.DEAD_BUSH, "枯れ木", RewardRule.WEEDER)
        ));
    }

    List<Entry> entries(Category category) {
        return entries.getOrDefault(category, List.of());
    }

    private static Entry e(Category category, Material material, String name, RewardRule rule) {
        return new Entry(category, material, name, rule);
    }
}
