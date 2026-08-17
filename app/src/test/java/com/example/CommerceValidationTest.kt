package com.example

import com.example.commerce.CommerceValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommerceValidationTest {
    @Test
    fun roundCurrencyUsesHalfUpToTwoDecimals() {
        assertEquals(12.35, CommerceValidation.roundCurrency(12.345), 0.0)
        assertEquals(12.34, CommerceValidation.roundCurrency(12.344), 0.0)
    }

    @Test
    fun calculatedLineAndBillTotalsUseRoundedMoney() {
        val lineOne = CommerceValidation.calculateLineTotal(12.345, 2.0)
        val lineTwo = CommerceValidation.calculateLineTotal(0.105, 1.0)

        assertEquals(24.70, lineOne, 0.0)
        assertEquals(0.11, lineTwo, 0.0)
        assertEquals(24.81, CommerceValidation.roundCurrency(lineOne + lineTwo), 0.0)
    }

    @Test
    fun amountsMatchRejectsNonFiniteValues() {
        assertTrue(CommerceValidation.amountsMatch(10.0, 10.0))
        assertFalse(CommerceValidation.amountsMatch(Double.NaN, 10.0))
        assertFalse(CommerceValidation.amountsMatch(10.0, Double.POSITIVE_INFINITY))
    }
}
