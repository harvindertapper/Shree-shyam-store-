package com.example

import com.example.commerce.CommerceValidation
import com.example.data.SaleItem
import com.example.utils.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommerceValidationTest {
    @Test
    fun majorUnitInputUsesHalfUpToIntegerPaise() {
        assertEquals(1235L, MoneyUtils.parseMajorUnits("12.345"))
        assertEquals(1234L, MoneyUtils.parseMajorUnits("12.344"))
        assertEquals(0L, MoneyUtils.parseMajorUnits("0.00"))
    }

    @Test
    fun calculatedLineAndBillTotalsUseIntegerPaise() {
        val lineOne = CommerceValidation.calculateLineTotal(1235L, 2.0)
        val lineTwo = CommerceValidation.calculateLineTotal(11L, 1.0)
        val items = listOf(
            SaleItem(productId = 1L, saleId = 1L, productNameSnapshot = "A", unitPrice = 1235L, lineTotal = lineOne, quantity = 2.0),
            SaleItem(productId = 2L, saleId = 1L, productNameSnapshot = "B", unitPrice = 11L, lineTotal = lineTwo, quantity = 1.0)
        )

        assertEquals(2470L, lineOne)
        assertEquals(11L, lineTwo)
        assertEquals(2481L, CommerceValidation.calculateBillTotal(items))
    }

    @Test
    fun amountsMatchUsesExactMinorUnitEquality() {
        assertTrue(CommerceValidation.amountsMatch(100L, 100L))
        assertFalse(CommerceValidation.amountsMatch(100L, 101L))
    }
}
