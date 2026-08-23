package com.sagakenichi.job;

import org.bukkit.entity.Player;

final class BeginnerService {

    private long durationMillis;
    private int multiplier;

    BeginnerService(long durationHours, int multiplier) {
        reload(durationHours, multiplier);
    }

    void reload(long durationHours, int multiplier) {
        this.durationMillis = Math.max(1L, durationHours) * 60L * 60L * 1000L;
        this.multiplier = Math.max(1, multiplier);
    }

    boolean isBeginner(Player player) {
        return isBeginner(player.getFirstPlayed(), System.currentTimeMillis());
    }

    boolean isBeginner(long firstPlayedMillis, long nowMillis) {
        if (firstPlayedMillis <= 0L) {
            return true;
        }
        long elapsed = Math.max(0L, nowMillis - firstPlayedMillis);
        return elapsed < durationMillis;
    }

    int multiplier(Player player) {
        return isBeginner(player) ? multiplier : 1;
    }

    long remainingMillis(Player player) {
        long firstPlayed = player.getFirstPlayed();
        if (firstPlayed <= 0L) {
            return durationMillis;
        }
        return Math.max(0L, durationMillis - Math.max(0L, System.currentTimeMillis() - firstPlayed));
    }

    int beginnerMultiplier() {
        return multiplier;
    }
}
