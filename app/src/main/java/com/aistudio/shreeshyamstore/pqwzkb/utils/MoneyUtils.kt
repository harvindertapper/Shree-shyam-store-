package com.aistudio.shreeshyamstore.pqwzkb.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Canonical money boundary for the commerce domain.
 *
 * Persisted amounts are integer Indian paise (100 paise = ₹1). Double values are
 * accepted only at legacy/UI boundaries and are immediately converted with
 * HALF_UP rounding to two decimal places.
 */
object MoneyUtils {
    const val MINOR_UNITS_PER_RUPEE = 100L
    private const val CURRENCY_SCALE = 2
    private val INDIA_LOCALE = Locale.Builder().setLanguage("en").setRegion("IN").build()
    private val DECIMAL_INPUT_PATTERN = Regex("^[+]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)$")

    fun parseMajorUnits(input: String): Long? {
        val normalized = input.trim()
        if (normalized.isEmpty() || !DECIMAL_INPUT_PATTERN.matches(normalized)) return null
        if (normalized.contains(',') && normalized.contains('.')) return null
        val canonical = normalized.replace(',', '.')
        return runCatching {
            BigDecimal(canonical)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
                .movePointRight(CURRENCY_SCALE)
                .longValueExact()
                .also { require(it >= 0L) }
        }.getOrNull()
    }

    fun fromMajor(value: Double): Long {
        require(value.isFinite()) { "Money value must be finite" }
        return BigDecimal.valueOf(value)
            .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
            .movePointRight(CURRENCY_SCALE)
            .longValueExact()
            .also { require(it >= 0L) { "Money value cannot be negative" } }
    }

    /** Converts an old cloud/backup major-unit number such as 12.35 to 1235 paise. */
    fun fromLegacyMajor(value: Double): Long = fromMajor(value)

    fun toMajorDecimal(minorUnits: Long): BigDecimal =
        BigDecimal.valueOf(minorUnits, CURRENCY_SCALE)

    fun toMajorDouble(minorUnits: Long): Double =
        toMajorDecimal(minorUnits).toDouble()

    fun toInputString(minorUnits: Long): String =
        toMajorDecimal(minorUnits).setScale(CURRENCY_SCALE, RoundingMode.UNNECESSARY).toPlainString()

    fun formatRupees(minorUnits: Long): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(INDIA_LOCALE)
            formatter.format(toMajorDecimal(minorUnits))
                .replace("Rs.", "₹")
                .replace("INR", "₹")
                .replace("Rs", "₹")
                .trim()
                .let { if (it.contains("₹")) it else "₹$it" }
        } catch (_: Exception) {
            "₹${toInputString(minorUnits)}"
        }
    }
}
