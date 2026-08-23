package com.sagakenichi.job;

enum RewardRule {
    MINER_STONE("採掘師", 16, 1),
    MINER_LOW_ORE("採掘師", 1, 2),
    MINER_MID_ORE("採掘師", 1, 4),
    MINER_HIGH_ORE("採掘師", 1, 12),
    LUMBERJACK("木こり", 8, 1),
    FARMER("農家", 8, 1),
    LAND_CLEARER("整地屋", 16, 1),
    WEEDER("雑草抜き", 128, 1);

    private final String displayName;
    private final int unitCount;
    private final int yenPerUnit;

    RewardRule(String displayName, int unitCount, int yenPerUnit) {
        this.displayName = displayName;
        this.unitCount = unitCount;
        this.yenPerUnit = yenPerUnit;
    }

    String displayName() {
        return displayName;
    }

    int unitCount() {
        return unitCount;
    }

    int yenPerUnit() {
        return yenPerUnit;
    }

    String progressKey() {
        return "progress_" + name().toLowerCase();
    }
}
