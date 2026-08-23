package com.sagakenichi.job;

import java.util.Arrays;
import java.util.Locale;

/**
 * Stable reward categories. The default values are only fallbacks; live values are
 * loaded from config.yml through {@link RateTable}.
 */
enum RewardRule {
    MINER_STONE("miner-stone", "採掘師: 石/丸石/深層岩など", 16, 1,
            "stone", "miner_stone"),
    MINER_LOW_ORE("miner-low-ore", "採掘師: 石炭/銅/鉄鉱石", 1, 2,
            "low-ore", "low_ore", "miner_low_ore"),
    MINER_MID_ORE("miner-mid-ore", "採掘師: 金/レッドストーン/ラピス鉱石", 1, 4,
            "mid-ore", "mid_ore", "miner_mid_ore"),
    MINER_HIGH_ORE("miner-high-ore", "採掘師: ダイヤ/エメラルド鉱石", 1, 12,
            "high-ore", "high_ore", "miner_high_ore"),
    LUMBERJACK("lumberjack", "木こり: 原木・木・幹・菌糸", 8, 1,
            "wood", "logger"),
    FARMER("farmer", "農家: 作物の収穫", 8, 1,
            "farm"),
    LAND_CLEARER("land-clearer", "整地屋: dirt/sand/gravel/clayなど", 16, 1,
            "land", "land_clearer"),
    WEEDER("weeder", "雑草抜き: grass/fern/tall_grass/dead_bushなど", 128, 1,
            "weed");

    private final String configKey;
    private final String description;
    private final int defaultUnitCount;
    private final int defaultYenPerUnit;
    private final String[] aliases;

    RewardRule(String configKey, String description, int defaultUnitCount, int defaultYenPerUnit,
               String... aliases) {
        this.configKey = configKey;
        this.description = description;
        this.defaultUnitCount = defaultUnitCount;
        this.defaultYenPerUnit = defaultYenPerUnit;
        this.aliases = aliases;
    }

    String configKey() { return configKey; }
    String description() { return description; }
    String displayName() {
        int colon = description.indexOf(':');
        return colon >= 0 ? description.substring(0, colon) : description;
    }
    int defaultUnitCount() { return defaultUnitCount; }
    int defaultYenPerUnit() { return defaultYenPerUnit; }
    String progressKey() { return "progress_" + name().toLowerCase(Locale.ROOT); }

    static RewardRule fromInput(String input) {
        if (input == null || input.isBlank()) return null;
        String normalized = input.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (RewardRule rule : values()) {
            if (rule.configKey.equals(normalized)
                    || rule.name().toLowerCase(Locale.ROOT).replace('_', '-').equals(normalized)
                    || Arrays.stream(rule.aliases)
                    .map(value -> value.toLowerCase(Locale.ROOT).replace('_', '-'))
                    .anyMatch(normalized::equals)) {
                return rule;
            }
        }
        return null;
    }
}
