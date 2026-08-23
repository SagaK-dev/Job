package com.sagakenichi.job;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;

final class BeginnerService {

    static final long TICKS_PER_SECOND = 20L;
    static final long TICKS_PER_MINUTE = 60L * TICKS_PER_SECOND;
    static final long TICKS_PER_HOUR = 60L * TICKS_PER_MINUTE;
    private static final long MAX_PLAYTIME_HOURS = Integer.MAX_VALUE / TICKS_PER_HOUR;
    private static final int MAX_MULTIPLIER = 100;

    private long playtimeHours;
    private long beginnerThresholdTicks;
    private int multiplier;

    BeginnerService(long playtimeHours, int multiplier) {
        reload(playtimeHours, multiplier);
    }

    void reload(long playtimeHours, int multiplier) {
        this.playtimeHours = Math.max(1L, Math.min(MAX_PLAYTIME_HOURS, playtimeHours));
        this.beginnerThresholdTicks = this.playtimeHours * TICKS_PER_HOUR;
        this.multiplier = Math.max(1, Math.min(MAX_MULTIPLIER, multiplier));
    }

    boolean isBeginner(Player player) {
        return isBeginnerTicks(playedTicks(player));
    }

    boolean isBeginnerTicks(long playedTicks) {
        return Math.max(0L, playedTicks) < beginnerThresholdTicks;
    }

    int multiplier(Player player) {
        return isBeginner(player) ? multiplier : 1;
    }

    long playedTicks(Player player) {
        return Math.max(0L, player.getStatistic(Statistic.PLAY_ONE_MINUTE));
    }

    long remainingTicks(Player player) {
        return Math.max(0L, beginnerThresholdTicks - playedTicks(player));
    }

    int beginnerMultiplier() {
        return multiplier;
    }

    long playtimeHours() {
        return playtimeHours;
    }

    long beginnerThresholdTicks() {
        return beginnerThresholdTicks;
    }
}
