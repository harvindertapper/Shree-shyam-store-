package com.aistudio.shreeshyamstore.pqwzkb.commerce

import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Pure financial rules shared by checkout validation and deterministic tests.
 * Monetary values are integer paise at this boundary; Double is retained only
 * for physical quantities because units such as kg and litres may be fractional.
 */
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
    PAYMENT,
    REVERSAL,
    CORRECTION
}

object CommerceValidation {
    const val CURRENCY_SCALE = 2

    fun normalizeUnitPrice(value: Long): Long {
        require(value >= 0L) { "Money value cannot be negative" }
        return value
    }

    fun calculateLineTotal(unitPrice: Long, quantity: Double): Long {
        require(quantity.isFinite() && quantity > 0.0) {
            "Quantity must be a finite positive amount"
        }
        require(unitPrice >= 0L) { "Money value cannot be negative" }
        return BigDecimal.valueOf(unitPrice)
            .multiply(BigDecimal.valueOf(quantity))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun calculateBillTotal(items: List<SaleItem>): Long =
        items.sumOf { calculateLineTotal(it.unitPrice, it.quantity) }

    fun amountsMatch(expected: Long, actual: Long): Boolean = expected == actual
}
