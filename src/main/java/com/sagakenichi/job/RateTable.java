package com.sagakenichi.job;

import java.util.EnumMap;
import java.util.Map;

/** Single source of truth for job rates. */
final class RateTable {
    static final int MAX_UNIT_COUNT = 1_000_000;
    static final int MAX_YEN_PER_UNIT = 1_000_000;

    record Rate(int unitCount, int yenPerUnit) {
        Rate {
            if (unitCount < 1 || unitCount > MAX_UNIT_COUNT) {
                throw new IllegalArgumentException("unitCount must be 1.." + MAX_UNIT_COUNT);
            }
            if (yenPerUnit < 1 || yenPerUnit > MAX_YEN_PER_UNIT) {
                throw new IllegalArgumentException("yenPerUnit must be 1.." + MAX_YEN_PER_UNIT);
            }
        }
    }

    private final JobPlugin plugin;
    private final Map<RewardRule, Rate> rates = new EnumMap<>(RewardRule.class);

    RateTable(JobPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    void reload() {
        rates.clear();
        for (RewardRule rule : RewardRule.values()) {
            String path = "rates." + rule.configKey();
            int unitCount = plugin.getConfig().getInt(path + ".unit-count", rule.defaultUnitCount());
            int yen = plugin.getConfig().getInt(path + ".yen", rule.defaultYenPerUnit());
            if (!isValidUnitCount(unitCount) || !isValidYen(yen)) {
                plugin.getLogger().warning("Invalid rate at " + path + "; using default "
                        + rule.defaultUnitCount() + " items = " + rule.defaultYenPerUnit() + " yen.");
                unitCount = rule.defaultUnitCount();
                yen = rule.defaultYenPerUnit();
            }
            rates.put(rule, new Rate(unitCount, yen));
        }
    }

    Rate rate(RewardRule rule) {
        Rate rate = rates.get(rule);
        if (rate == null) throw new IllegalStateException("Rate was not loaded for " + rule);
        return rate;
    }

    void set(RewardRule rule, int unitCount, int yenPerUnit) {
        Rate rate = new Rate(unitCount, yenPerUnit);
        String path = "rates." + rule.configKey();
        plugin.getConfig().set(path + ".unit-count", unitCount);
        plugin.getConfig().set(path + ".yen", yenPerUnit);
        plugin.saveConfig();
        rates.put(rule, rate);
    }

    static boolean isValidUnitCount(int value) { return value >= 1 && value <= MAX_UNIT_COUNT; }
    static boolean isValidYen(int value) { return value >= 1 && value <= MAX_YEN_PER_UNIT; }
}
