package com.sevenzenlabs.zenmart.commerce

import com.sevenzenlabs.zenmart.data.Return
import com.sevenzenlabs.zenmart.data.Sale
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * A calendar date with no timezone attached. Date-picker values are converted to
 * this type before a report range is built, preventing device timezone shifts.
 */
data class ReportDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    fun atStartOfDayMillis(timeZone: TimeZone): Long =
        Calendar.getInstance(timeZone).apply {
            clear()
            isLenient = false
            set(year, month - 1, day, 0, 0, 0)
        }.timeInMillis

    fun displayValue(): String = "%02d/%02d/%04d".format(Locale.ENGLISH, day, month, year)

    fun toDatePickerMillis(): Long = Calendar.getInstance(UTC).apply {
        clear()
        set(year, month - 1, day, 0, 0, 0)
    }.timeInMillis

    companion object {
        fun fromDatePickerMillis(selectedDateMillis: Long): ReportDate {
            val calendar = Calendar.getInstance(UTC).apply {
                timeInMillis = selectedDateMillis
            }
            return ReportDate(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH)
            )
        }

        private val UTC = TimeZone.getTimeZone("UTC")
    }
}

data class ReportDateRange(
    val startInclusiveMillis: Long?,
    val endExclusiveMillis: Long?
) {
    init {
        require(
            startInclusiveMillis == null ||
                endExclusiveMillis == null ||
                startInclusiveMillis < endExclusiveMillis
        ) { "Report range must use an exclusive end after its inclusive start" }
    }

    fun contains(timestampMillis: Long): Boolean =
        (startInclusiveMillis == null || timestampMillis >= startInclusiveMillis) &&
            (endExclusiveMillis == null || timestampMillis < endExclusiveMillis)
}

enum class ReportInterval {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME,
    CUSTOM
}

enum class ReportRangeError {
    START_DATE_REQUIRED,
    END_DATE_REQUIRED,
    START_DATE_AFTER_END_DATE
}

sealed interface ReportRangeResult {
    data class Valid(val range: ReportDateRange) : ReportRangeResult
    data class Invalid(val error: ReportRangeError) : ReportRangeResult
}

/**
 * Report inclusion policy: deleted records and FAILED payment states are excluded
 * from revenue. PENDING, RECEIVED, NOT_REQUIRED, PARTIALLY_REFUNDED, and REFUNDED
 * sales are included as gross recorded sales. Returns and refunds are tracked as
 * separate event deductions to calculate net sales and avoid double-deducting refunds.
 * Invalid payment-state values are excluded rather than guessed.
 */
object ReportPolicy {
    fun resolveRange(
        interval: ReportInterval,
        nowMillis: Long,
        timeZone: TimeZone,
        customStart: ReportDate? = null,
        customEnd: ReportDate? = null
    ): ReportRangeResult {
        if (interval == ReportInterval.CUSTOM) {
            if (customStart == null) return ReportRangeResult.Invalid(ReportRangeError.START_DATE_REQUIRED)
            if (customEnd == null) return ReportRangeResult.Invalid(ReportRangeError.END_DATE_REQUIRED)
            if (compare(customStart, customEnd) > 0) {
                return ReportRangeResult.Invalid(ReportRangeError.START_DATE_AFTER_END_DATE)
            }
            return ReportRangeResult.Valid(
                ReportDateRange(
                    startInclusiveMillis = customStart.atStartOfDayMillis(timeZone),
                    endExclusiveMillis = nextDay(customEnd, timeZone)
                )
            )
        }

        if (interval == ReportInterval.ALL_TIME) {
            return ReportRangeResult.Valid(
                ReportDateRange(startInclusiveMillis = null, endExclusiveMillis = null)
            )
        }

        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val today = ReportDate(
            year = now.get(Calendar.YEAR),
            month = now.get(Calendar.MONTH) + 1,
            day = now.get(Calendar.DAY_OF_MONTH)
        )
        val startDate = when (interval) {
            ReportInterval.TODAY -> today
            ReportInterval.THIS_WEEK -> mondayOfWeek(today, now.get(Calendar.DAY_OF_WEEK))
            ReportInterval.THIS_MONTH -> ReportDate(today.year, today.month, 1)
            ReportInterval.ALL_TIME, ReportInterval.CUSTOM -> error("Handled above")
        }
        val endDate = when (interval) {
            ReportInterval.TODAY -> today
            ReportInterval.THIS_WEEK -> addDays(startDate, 6, timeZone)
            ReportInterval.THIS_MONTH -> lastDayOfMonth(today, timeZone)
            ReportInterval.ALL_TIME, ReportInterval.CUSTOM -> error("Handled above")
        }
        return ReportRangeResult.Valid(
            ReportDateRange(
                startInclusiveMillis = startDate.atStartOfDayMillis(timeZone),
                endExclusiveMillis = nextDay(endDate, timeZone)
            )
        )
    }

    fun filterSales(sales: Iterable<Sale>, range: ReportDateRange): List<Sale> =
        sales.filter { sale ->
            isIncludedSale(sale) && range.contains(sale.createdAt)
        }

    fun filterReturns(returns: Iterable<Return>, range: ReportDateRange): List<Return> =
        returns.filter { ret ->
            isIncludedReturn(ret) && range.contains(ret.createdAt)
        }

    fun summarize(
        sales: Iterable<Sale>,
        returns: Iterable<Return> = emptyList()
    ): ReportSummary {
        val includedSales = sales.filter(::isIncludedSale)
        val includedReturns = returns.filter(::isIncludedReturn)

        val grossSales = includedSales.sumOf { it.totalAmount }
        val totalRefunds = includedReturns.sumOf { it.totalRefundAmount }
        val netSales = grossSales - totalRefunds

        val cashGross = includedSales.filter { it.paymentMode.equals("CASH", ignoreCase = true) }.sumOf { it.totalAmount }
        val cashRefunds = includedReturns.filter { it.refundMode.equals("CASH", ignoreCase = true) }.sumOf { it.totalRefundAmount }
        val cashNet = cashGross - cashRefunds

        val upiGross = includedSales.filter { it.paymentMode.equals("UPI", ignoreCase = true) }.sumOf { it.totalAmount }
        val upiRefunds = includedReturns.filter { it.refundMode.equals("UPI", ignoreCase = true) }.sumOf { it.totalRefundAmount }
        val upiNet = upiGross - upiRefunds

        val udhaarGross = includedSales.filter { it.paymentMode.equals("UDHAAR", ignoreCase = true) }.sumOf { it.totalAmount }
        val udhaarRefunds = includedReturns.filter { it.refundMode.startsWith("UDHAAR", ignoreCase = true) }.sumOf { it.totalRefundAmount }
        val udhaarNet = udhaarGross - udhaarRefunds

        return ReportSummary(
            grossSalesPaise = grossSales,
            totalRefundsPaise = totalRefunds,
            netSalesPaise = netSales,
            cashGrossPaise = cashGross,
            cashRefundsPaise = cashRefunds,
            cashNetPaise = cashNet,
            upiGrossPaise = upiGross,
            upiRefundsPaise = upiRefunds,
            upiNetPaise = upiNet,
            udhaarGrossPaise = udhaarGross,
            udhaarRefundsPaise = udhaarRefunds,
            udhaarNetPaise = udhaarNet,
            billsCount = includedSales.size,
            returnsCount = includedReturns.size
        )
    }

    private fun isIncludedSale(sale: Sale): Boolean {
        if (sale.isDeleted || sale.totalAmount <= 0L) return false
        return runCatching { PaymentState.fromWireValue(sale.paymentState) }
            .getOrNull()
            ?.let { state -> state != PaymentState.FAILED }
            ?: false
    }

    private fun isIncludedReturn(ret: Return): Boolean {
        return !ret.isDeleted && ret.totalRefundAmount > 0L
    }

    private fun nextDay(date: ReportDate, timeZone: TimeZone): Long =
        addDays(date, 1, timeZone).atStartOfDayMillis(timeZone)

    private fun mondayOfWeek(date: ReportDate, dayOfWeek: Int): ReportDate {
        val daysSinceMonday = (dayOfWeek + 5) % 7
        return addDays(date, -daysSinceMonday, UTC)
    }

    private fun lastDayOfMonth(date: ReportDate, timeZone: TimeZone): ReportDate {
        val calendar = Calendar.getInstance(timeZone).apply {
            clear()
            set(date.year, date.month - 1, 1)
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return ReportDate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun addDays(date: ReportDate, days: Int, timeZone: TimeZone): ReportDate {
        val calendar = Calendar.getInstance(timeZone).apply {
            clear()
            set(date.year, date.month - 1, date.day)
            add(Calendar.DAY_OF_MONTH, days)
        }
        return ReportDate(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun compare(left: ReportDate, right: ReportDate): Int =
        compareValuesBy(left, right, ReportDate::year, ReportDate::month, ReportDate::day)

    private val UTC = TimeZone.getTimeZone("UTC")
}

data class ReportSummary(
    val grossSalesPaise: Long,
    val totalRefundsPaise: Long,
    val netSalesPaise: Long,
    val cashGrossPaise: Long,
    val cashRefundsPaise: Long,
    val cashNetPaise: Long,
    val upiGrossPaise: Long,
    val upiRefundsPaise: Long,
    val upiNetPaise: Long,
    val udhaarGrossPaise: Long,
    val udhaarRefundsPaise: Long,
    val udhaarNetPaise: Long,
    val billsCount: Int,
    val returnsCount: Int = 0,
    val totalRevenuePaise: Long = netSalesPaise,
    val cashRevenuePaise: Long = cashNetPaise,
    val upiRevenuePaise: Long = upiNetPaise,
    val udhaarRevenuePaise: Long = udhaarNetPaise
)
