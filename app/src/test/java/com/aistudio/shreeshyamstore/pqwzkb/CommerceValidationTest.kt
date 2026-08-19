package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.utils.MoneyUtils
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

    @Test
    fun cashMayRecordChangeButUpiMustMatchExactly() {
        assertEquals(
            PaymentState.RECEIVED to 1500L,
            CommerceValidation.validateCheckoutPayment("CASH", 1000L, 1500L)
        )
        assertEquals(
            PaymentState.RECEIVED to 1000L,
            CommerceValidation.validateCheckoutPayment("UPI", 1000L, 1000L)
        )
        assertTrue(runCatching {
            CommerceValidation.validateCheckoutPayment("UPI", 1000L, 999L)
        }.isFailure)
    }

    @Test
    fun udhaarHasNoReceivedSettlementAndPaymentStateCannotRegress() {
        assertEquals(
            PaymentState.NOT_REQUIRED to 0L,
            CommerceValidation.validateCheckoutPayment("UDHAAR", 1000L, 0L)
        )
        assertTrue(runCatching {
            CommerceValidation.validateCheckoutPayment("UDHAAR", 1000L, 1L)
        }.isFailure)
        assertTrue(runCatching {
            CommerceValidation.validatePaymentStateTransition(
                paymentModeRaw = "CASH",
                currentStateRaw = PaymentState.RECEIVED.wireValue,
                targetState = PaymentState.FAILED,
                totalAmount = 1000L,
                receivedAmount = 0L
            )
        }.isFailure)
    }
}
