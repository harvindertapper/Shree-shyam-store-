package com.sevenzenlabs.zenmart

import com.sevenzenlabs.zenmart.commerce.PaymentState
import com.sevenzenlabs.zenmart.commerce.ReportDate
import com.sevenzenlabs.zenmart.commerce.ReportDateRange
import com.sevenzenlabs.zenmart.commerce.ReportInterval
import com.sevenzenlabs.zenmart.commerce.ReportPolicy
import com.sevenzenlabs.zenmart.commerce.ReportRangeError
import com.sevenzenlabs.zenmart.commerce.ReportRangeResult
import com.sevenzenlabs.zenmart.data.Return
import com.sevenzenlabs.zenmart.data.Sale
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
    fun filterExcludesDeletedFailedAndInvalidSalesButIncludesPendingAndRefundedBills() {
        val start = localMillis(ReportDate(2026, 8, 25), 0)
        val range = ReportDateRange(start, start + 24 * 60 * 60 * 1000L)
        val sales = listOf(
            sale("cash", start + 1, PaymentState.NOT_REQUIRED.wireValue, total = 100L),
            sale("pending", start + 2, PaymentState.PENDING.wireValue, total = 200L),
            sale("received", start + 3, PaymentState.RECEIVED.wireValue, total = 300L),
            sale("failed", start + 4, PaymentState.FAILED.wireValue, total = 400L),
            sale("refunded", start + 5, PaymentState.REFUNDED.wireValue, total = 500L),
            sale("partially-refunded", start + 6, PaymentState.PARTIALLY_REFUNDED.wireValue, total = 600L),
            sale("invalid", start + 7, "UNKNOWN", total = 700L),
            sale("deleted", start + 8, PaymentState.RECEIVED.wireValue, total = 800L, isDeleted = true),
            sale("after-range", range.endExclusiveMillis!!, PaymentState.RECEIVED.wireValue, total = 900L)
        )

        assertEquals(
            listOf("cash", "pending", "received", "refunded", "partially-refunded"),
            ReportPolicy.filterSales(sales, range).map { it.billNumber }
        )
    }

    @Test
    fun filterReturnsExcludesDeletedAndZeroAmountReturnsAndRespectsDateRange() {
        val start = localMillis(ReportDate(2026, 8, 25), 0)
        val range = ReportDateRange(start, start + 24 * 60 * 60 * 1000L)
        val returns = listOf(
            returnRecord("ret-1", start + 10, totalRefundAmount = 150L),
            returnRecord("ret-2", start + 20, totalRefundAmount = 0L),
            returnRecord("ret-3", start + 30, totalRefundAmount = 250L, isDeleted = true),
            returnRecord("ret-4", range.endExclusiveMillis!!, totalRefundAmount = 350L)
        )

        val filtered = ReportPolicy.filterReturns(returns, range)
        assertEquals(listOf("ret-1"), filtered.map { it.returnNumber })
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

        assertEquals(750L, summary.grossSalesPaise)
        assertEquals(0L, summary.totalRefundsPaise)
        assertEquals(750L, summary.netSalesPaise)
        assertEquals(750L, summary.totalRevenuePaise)
        assertEquals(125L, summary.cashRevenuePaise)
        assertEquals(250L, summary.upiRevenuePaise)
        assertEquals(375L, summary.udhaarRevenuePaise)
        assertEquals(3, summary.billsCount)
        assertEquals(0, summary.returnsCount)
    }

    @Test
    fun summarizeCalculatesGrossRefundsAndNetReconciledSalesAcrossPaymentModes() {
        val sales = listOf(
            sale("cash-sale", 1L, PaymentState.RECEIVED.wireValue, "CASH", 5000L),
            sale("upi-sale", 2L, PaymentState.RECEIVED.wireValue, "UPI", 3000L),
            sale("udhaar-sale", 3L, PaymentState.PENDING.wireValue, "UDHAAR", 2000L)
        )
        val returns = listOf(
            returnRecord("ret-cash", 4L, totalRefundAmount = 1000L, refundMode = "CASH"),
            returnRecord("ret-upi", 5L, totalRefundAmount = 500L, refundMode = "UPI"),
            returnRecord("ret-udhaar", 6L, totalRefundAmount = 400L, refundMode = "UDHAAR_REVERSAL")
        )

        val summary = ReportPolicy.summarize(sales, returns)

        // Gross amounts
        assertEquals(10000L, summary.grossSalesPaise)
        assertEquals(5000L, summary.cashGrossPaise)
        assertEquals(3000L, summary.upiGrossPaise)
        assertEquals(2000L, summary.udhaarGrossPaise)

        // Refund amounts
        assertEquals(1900L, summary.totalRefundsPaise)
        assertEquals(1000L, summary.cashRefundsPaise)
        assertEquals(500L, summary.upiRefundsPaise)
        assertEquals(400L, summary.udhaarRefundsPaise)

        // Net reconciled amounts
        assertEquals(8100L, summary.netSalesPaise)
        assertEquals(4000L, summary.cashNetPaise)
        assertEquals(2500L, summary.upiNetPaise)
        assertEquals(1600L, summary.udhaarNetPaise)

        // Invariant: Net Sales == Cash Net + UPI Net + Udhaar Net
        assertEquals(
            summary.netSalesPaise,
            summary.cashNetPaise + summary.upiNetPaise + summary.udhaarNetPaise
        )

        // Invariant: Net Sales == Gross Sales - Total Refunds
        assertEquals(
            summary.netSalesPaise,
            summary.grossSalesPaise - summary.totalRefundsPaise
        )

        // Backward-compatible getters
        assertEquals(summary.netSalesPaise, summary.totalRevenuePaise)
        assertEquals(summary.cashNetPaise, summary.cashRevenuePaise)
        assertEquals(summary.upiNetPaise, summary.upiRevenuePaise)
        assertEquals(summary.udhaarNetPaise, summary.udhaarRevenuePaise)

        assertEquals(3, summary.billsCount)
        assertEquals(3, summary.returnsCount)
    }

    @Test
    fun summarizeReconcilesPartialReturnsCorrectlyWithoutDoubleCounting() {
        val sales = listOf(
            sale("bill-100", 1000L, PaymentState.PARTIALLY_REFUNDED.wireValue, "CASH", 10000L)
        )
        val returns = listOf(
            returnRecord("ret-p1", 1050L, totalRefundAmount = 3000L, refundMode = "CASH")
        )

        val summary = ReportPolicy.summarize(sales, returns)

        assertEquals(10000L, summary.grossSalesPaise)
        assertEquals(3000L, summary.totalRefundsPaise)
        assertEquals(7000L, summary.netSalesPaise)
        assertEquals(10000L, summary.cashGrossPaise)
        assertEquals(3000L, summary.cashRefundsPaise)
        assertEquals(7000L, summary.cashNetPaise)
        assertEquals(1, summary.billsCount)
        assertEquals(1, summary.returnsCount)
    }

    @Test
    fun crossDateReturnReconcilesCorrectlyAcrossSaleAndReturnDates() {
        val day1Start = localMillis(ReportDate(2026, 8, 25), 0)
        val day1Range = ReportDateRange(day1Start, day1Start + 24 * 60 * 60 * 1000L)

        val day2Start = day1Range.endExclusiveMillis!!
        val day2Range = ReportDateRange(day2Start, day2Start + 24 * 60 * 60 * 1000L)

        val combinedRange = ReportDateRange(day1Start, day2Range.endExclusiveMillis)

        val sales = listOf(
            sale("bill-day1", day1Start + 1000L, PaymentState.REFUNDED.wireValue, "UPI", 2500L)
        )
        val returns = listOf(
            returnRecord("ret-day2", day2Start + 2000L, totalRefundAmount = 2500L, refundMode = "UPI")
        )

        // Day 1: Gross 2500, Returns 0, Net 2500
        val day1Sales = ReportPolicy.filterSales(sales, day1Range)
        val day1Returns = ReportPolicy.filterReturns(returns, day1Range)
        val day1Summary = ReportPolicy.summarize(day1Sales, day1Returns)
        assertEquals(2500L, day1Summary.grossSalesPaise)
        assertEquals(0L, day1Summary.totalRefundsPaise)
        assertEquals(2500L, day1Summary.netSalesPaise)

        // Day 2: Gross 0, Returns 2500, Net -2500
        val day2Sales = ReportPolicy.filterSales(sales, day2Range)
        val day2Returns = ReportPolicy.filterReturns(returns, day2Range)
        val day2Summary = ReportPolicy.summarize(day2Sales, day2Returns)
        assertEquals(0L, day2Summary.grossSalesPaise)
        assertEquals(2500L, day2Summary.totalRefundsPaise)
        assertEquals(-2500L, day2Summary.netSalesPaise)

        // Combined 2-day period: Gross 2500, Returns 2500, Net 0 (Fully reconciled, no double counting)
        val combinedSales = ReportPolicy.filterSales(sales, combinedRange)
        val combinedReturns = ReportPolicy.filterReturns(returns, combinedRange)
        val combinedSummary = ReportPolicy.summarize(combinedSales, combinedReturns)
        assertEquals(2500L, combinedSummary.grossSalesPaise)
        assertEquals(2500L, combinedSummary.totalRefundsPaise)
        assertEquals(0L, combinedSummary.netSalesPaise)
        assertEquals(1, combinedSummary.billsCount)
        assertEquals(1, combinedSummary.returnsCount)
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

    private fun returnRecord(
        returnNumber: String,
        createdAt: Long,
        totalRefundAmount: Long,
        refundMode: String = "CASH",
        isDeleted: Boolean = false
    ): Return = Return(
        returnNumber = returnNumber,
        saleId = 1L,
        originalBillNumber = "BILL-001",
        totalRefundAmount = totalRefundAmount,
        refundMode = refundMode,
        createdAt = createdAt,
        updatedAt = createdAt,
        isDeleted = isDeleted
    )
}
