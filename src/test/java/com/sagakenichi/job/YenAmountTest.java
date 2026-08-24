package com.sagakenichi.job;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class YenAmountTest {

    @Test
    void wholeJobYenKeepsExistingSemantics() {
        assertEquals(100L, YenAmount.wholeYenToMinor(1L));
        assertEquals(12_300L, YenAmount.wholeYenToMinor(123L));
        assertEquals(123L, YenAmount.minorToWholeYen(12_399L));
        assertEquals("1,234円", YenAmount.formatMinor(123_400L));
    }

    @Test
    void fractionalVaultPricesArePreservedToTwoDigits() {
        assertEquals(25L, YenAmount.decimalYenToMinor(0.25D));
        assertEquals(250L, YenAmount.decimalYenToMinor(2.5D));
        assertEquals(1234.25D, YenAmount.minorToDecimalYen(123_425L), 1.0E-9);
        assertEquals("1,234.25円", YenAmount.formatMinor(123_425L));
    }

    @Test
    void vaultAmountsUseStableHalfUpRounding() {
        assertEquals(101L, YenAmount.decimalYenToMinor(1.005D));
        assertEquals(100L, YenAmount.decimalYenToMinor(1.004D));
        assertThrows(IllegalArgumentException.class, () -> YenAmount.decimalYenToMinor(-1.0D));
        assertThrows(IllegalArgumentException.class, () -> YenAmount.decimalYenToMinor(Double.NaN));
    }
}
