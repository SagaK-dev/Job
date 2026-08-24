package com.sagakenichi.job;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/** Fixed-point helpers for Job yen. One yen is stored as 100 minor units. */
final class YenAmount {
    static final long MINOR_PER_YEN = 100L;
    static final int FRACTIONAL_DIGITS = 2;

    private YenAmount() {
    }

    static long wholeYenToMinor(long yen) {
        if (yen < 0L) throw new IllegalArgumentException("yen must be non-negative");
        return Math.multiplyExact(yen, MINOR_PER_YEN);
    }

    static long decimalYenToMinor(double yen) {
        if (!Double.isFinite(yen) || yen < 0.0D) {
            throw new IllegalArgumentException("yen must be finite and non-negative");
        }
        BigDecimal rounded = BigDecimal.valueOf(yen).setScale(FRACTIONAL_DIGITS, RoundingMode.HALF_UP);
        return rounded.movePointRight(FRACTIONAL_DIGITS).longValueExact();
    }

    static double minorToDecimalYen(long minor) {
        if (minor <= 0L) return 0.0D;
        return BigDecimal.valueOf(minor, FRACTIONAL_DIGITS).doubleValue();
    }

    static long minorToWholeYen(long minor) {
        return Math.max(0L, minor) / MINOR_PER_YEN;
    }

    static String formatMinor(long minor) {
        BigDecimal value = BigDecimal.valueOf(Math.max(0L, minor), FRACTIONAL_DIGITS).stripTrailingZeros();
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return format.format(value) + "円";
    }

    static String formatDecimal(double yen) {
        return formatMinor(decimalYenToMinor(yen));
    }
}
