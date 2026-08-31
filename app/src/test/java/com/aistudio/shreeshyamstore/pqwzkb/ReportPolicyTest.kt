package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentState
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportDate
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportDateRange
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportInterval
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportPolicy
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportRangeError
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportRangeResult
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReportPolicyTest {
    private val kolkata = TimeZone.getTimeZone("Asia/Kolkata")

    @Test
    fun customRangeUsesInclusiveStartAndExclusiveEndInDeviceTimezone() {
        val range = validRange(
            ReportPolicy.resolveRange(
                interval = ReportInterval.CUSTOM,
                nowMillis = localMillis(ReportDate(2026, 8, 26), 12),
                timeZone = kolkata,
                customStart = ReportDate(2026, 8, 25),
                customEnd = ReportDate(2026, 8, 25)
            )
        )

        assertTrue(range.contains(range.startInclusiveMillis!!))
        assertTrue(range.contains(range.endExclusiveMillis!! - 1L))
        assertFalse(range.contains(range.endExclusiveMillis!!))
        assertFalse(range.contains(range.startInclusiveMillis - 1L))
    }

    @Test
    fun datePickerRoundTripPreservesCalendarDate() {
        val selected = ReportDate(2026, 12, 31)
        assertEquals(selected, ReportDate.fromDatePickerMillis(selected.toDatePickerMillis()))
    }

    @Test
    fun thisWeekStartsOnMondayAndEndsAtNextMonday() {
        val range = validRange(
            ReportPolicy.resolveRange(
                interval = ReportInterval.THIS_WEEK,
                nowMillis = localMillis(ReportDate(2026, 8, 26), 12),
                timeZone = kolkata
            )
        )

        assertEquals(localMillis(ReportDate(2026, 8, 24), 0), range.startInclusiveMillis)
        assertEquals(localMillis(ReportDate(2026, 8, 31), 0), range.endExclusiveMillis)
    }

    @Test
    fun thisMonthUsesTheNextMonthAsExclusiveEnd() {
        val range = validRange(
            ReportPolicy.resolveRange(
                interval = ReportInterval.THIS_MONTH,
                nowMillis = localMillis(ReportDate(2026, 2, 15), 12),
                timeZone = kolkata
            )
        )

        assertEquals(localMillis(ReportDate(2026, 2, 1), 0), range.startInclusiveMillis)
        assertEquals(localMillis(ReportDate(2026, 3, 1), 0), range.endExclusiveMillis)
    }

    @Test
    fun customRangeRejectsMissingAndReversedDates() {
        val now = localMillis(ReportDate(2026, 8, 26), 12)
        assertEquals(
            ReportRangeError.START_DATE_REQUIRED,
            invalidError(ReportPolicy.resolveRange(ReportInterval.CUSTOM, now, kolkata))
        )
        assertEquals(
            ReportRangeError.END_DATE_REQUIRED,
            invalidError(
                ReportPolicy.resolveRange(
                    ReportInterval.CUSTOM,
                    now,
                    kolkata,
                    customStart = ReportDate(2026, 8, 26)
                )
            )
        )
        assertEquals(
            ReportRangeError.START_DATE_AFTER_END_DATE,
            invalidError(
                ReportPolicy.resolveRange(
                    ReportInterval.CUSTOM,
                    now,
                    kolkata,
                    customStart = ReportDate(2026, 8, 27),
                    customEnd = ReportDate(2026, 8, 26)
                )
            )
        )
    }

    @Test
    fun filterExcludesDeletedFailedRefundedAndInvalidSalesButIncludesPending() {
        val start = localMillis(ReportDate(2026, 8, 25), 0)
        val range = ReportDateRange(start, start + 24 * 60 * 60 * 1000L)
        val sales = listOf(
            sale("cash", start + 1, PaymentState.NOT_REQUIRED.wireValue, total = 100L),
            sale("pending", start + 2, PaymentState.PENDING.wireValue, total = 200L),
            sale("received", start + 3, PaymentState.RECEIVED.wireValue, total = 300L),
            sale("failed", start + 4, PaymentState.FAILED.wireValue, total = 400L),
            sale("refunded", start + 5, PaymentState.REFUNDED.wireValue, total = 500L),
            sale("invalid", start + 6, "UNKNOWN", total = 600L),
            sale("deleted", start + 7, PaymentState.RECEIVED.wireValue, total = 700L, isDeleted = true),
            sale("after-range", range.endExclusiveMillis!!, PaymentState.RECEIVED.wireValue, total = 800L)
        )

        assertEquals(listOf("cash", "pending", "received"), ReportPolicy.filterSales(sales, range).map { it.billNumber })
    }

    @Test
    fun summarizeAggregatesOnlyEligibleSalesInIntegerPaise() {
        val sales = listOf(
            sale("cash", 1L, PaymentState.NOT_REQUIRED.wireValue, "CASH", 125L),
            sale("upi", 2L, PaymentState.RECEIVED.wireValue, "UPI", 250L),
            sale("udhaar", 3L, PaymentState.PENDING.wireValue, "UDHAAR", 375L),
            sale("failed", 4L, PaymentState.FAILED.wireValue, "CASH", 999L)
        )

        val summary = ReportPolicy.summarize(sales)

        assertEquals(750L, summary.totalRevenuePaise)
        assertEquals(125L, summary.cashRevenuePaise)
        assertEquals(250L, summary.upiRevenuePaise)
        assertEquals(375L, summary.udhaarRevenuePaise)
        assertEquals(3, summary.billsCount)
    }

    private fun validRange(result: ReportRangeResult): ReportDateRange = when (result) {
        is ReportRangeResult.Valid -> result.range
        is ReportRangeResult.Invalid -> error("Expected valid range but received ${result.error}")
    }

    private fun invalidError(result: ReportRangeResult): ReportRangeError = when (result) {
        is ReportRangeResult.Valid -> error("Expected invalid range")
        is ReportRangeResult.Invalid -> result.error
    }

    private fun localMillis(date: ReportDate, hour: Int): Long = Calendar.getInstance(kolkata).apply {
        clear()
        set(date.year, date.month - 1, date.day, hour, 0, 0)
    }.timeInMillis

    private fun sale(
        billNumber: String,
        createdAt: Long,
        paymentState: String,
        paymentMode: String = "CASH",
        total: Long,
        isDeleted: Boolean = false
    ): Sale = Sale(
        billNumber = billNumber,
        totalAmount = total,
        paymentMode = paymentMode,
        paymentState = paymentState,
        createdAt = createdAt,
        updatedAt = createdAt,
        isDeleted = isDeleted
    )
}
