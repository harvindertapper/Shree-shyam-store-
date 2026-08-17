package com.example.commerce

import com.example.data.SaleItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

enum class PaymentMode {
    CASH,
    UPI,
    UDHAAR;

    companion object {
        fun parse(raw: String): PaymentMode = values().firstOrNull {
            it.name == raw.trim().uppercase(Locale.ENGLISH)
        } ?: throw IllegalArgumentException("Unsupported payment mode: $raw")
    }
}

enum class UdhaarTransactionType {
    CREDIT,
    PAYMENT
}

/**
 * Pure financial rules shared by checkout validation and deterministic tests.
 * Existing persistence remains Double-compatible in this slice; all new
 * checkout calculations are normalized to two decimal places with HALF_UP
 * rounding until the schema can migrate to minor-unit money values.
 */
object CommerceValidation {
    const val CURRENCY_SCALE = 2
    private const val CURRENCY_COMPARISON_EPSILON = 0.000001

    fun roundCurrency(value: Double): Double {
        require(value.isFinite()) { "Money value must be finite" }
        return BigDecimal.valueOf(value)
            .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun normalizeUnitPrice(value: Double): Double = roundCurrency(value)

    fun calculateLineTotal(unitPrice: Double, quantity: Double): Double {
        require(quantity.isFinite() && quantity > 0.0) {
            "Quantity must be a finite positive amount"
        }
        return roundCurrency(normalizeUnitPrice(unitPrice) * quantity)
    }

    fun calculateBillTotal(items: List<SaleItem>): Double =
        roundCurrency(items.sumOf { calculateLineTotal(it.unitPrice, it.quantity) })

    fun amountsMatch(expected: Double, actual: Double): Boolean {
        if (!expected.isFinite() || !actual.isFinite()) return false
        return abs(roundCurrency(expected) - roundCurrency(actual)) < CURRENCY_COMPARISON_EPSILON
    }
}
