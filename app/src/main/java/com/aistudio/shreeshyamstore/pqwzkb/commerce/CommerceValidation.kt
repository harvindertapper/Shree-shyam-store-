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

    /**
     * Validates the amount physically received for a completed checkout and
     * derives the persisted lifecycle state shared with future Control Plane APIs.
     * Cash may exceed the total because the difference is change; UPI is exact;
     * udhaar has no payment collected at checkout.
     */
    fun validateCheckoutPayment(
        paymentModeRaw: String,
        totalAmount: Long,
        receivedAmount: Long?
    ): Pair<PaymentState, Long> {
        require(totalAmount >= 0L) { "Sale total must be a non-negative amount" }
        val mode = PaymentMode.parse(paymentModeRaw)
        val received = receivedAmount ?: if (mode == PaymentMode.UDHAAR) 0L else totalAmount
        require(received >= 0L) { "Received payment cannot be negative" }
        return when (mode) {
            PaymentMode.CASH -> {
                require(received >= totalAmount) {
                    "Cash received cannot be less than the bill total"
                }
                PaymentState.RECEIVED to received
            }
            PaymentMode.UPI -> {
                require(received == totalAmount) {
                    "UPI payment must match the bill total"
                }
                PaymentState.RECEIVED to received
            }
            PaymentMode.UDHAAR -> {
                require(received == 0L) {
                    "Udhaar checkout cannot record a received payment"
                }
                PaymentState.NOT_REQUIRED to 0L
            }
        }
    }

    fun validatePaymentStateTransition(
        paymentModeRaw: String,
        currentStateRaw: String,
        targetState: PaymentState,
        totalAmount: Long,
        receivedAmount: Long
    ): Long {
        val mode = PaymentMode.parse(paymentModeRaw)
        val currentState = PaymentState.fromWireValue(currentStateRaw)
        require(totalAmount >= 0L) { "Sale total must be a non-negative amount" }
        require(receivedAmount >= 0L) { "Received payment cannot be negative" }
        when (currentState) {
            PaymentState.PENDING -> require(targetState == PaymentState.RECEIVED || targetState == PaymentState.FAILED) {
                "Pending payment can only become received or failed"
            }
            PaymentState.FAILED -> require(targetState == PaymentState.RECEIVED || targetState == PaymentState.FAILED) {
                "Failed payment can only be retried or remain failed"
            }
            PaymentState.RECEIVED -> require(targetState == PaymentState.RECEIVED) {
                "A received payment cannot regress"
            }
            PaymentState.NOT_REQUIRED -> require(targetState == PaymentState.NOT_REQUIRED) {
                "A non-payment sale cannot enter a payment state"
            }
            PaymentState.PARTIALLY_REFUNDED,
            PaymentState.REFUNDED -> require(targetState == currentState) {
                "Refunded payment states are immutable in this slice"
            }
        }
        if (mode == PaymentMode.UDHAAR) {
            require(targetState == PaymentState.NOT_REQUIRED && receivedAmount == 0L) {
                "Udhaar sales do not accept settlement payments"
            }
            return 0L
        }
        require(targetState != PaymentState.NOT_REQUIRED) {
            "Cash and UPI sales require a payment state"
        }
        if (targetState == PaymentState.FAILED) {
            require(receivedAmount == 0L) { "Failed payment cannot record received money" }
            return 0L
        }
        return validateCheckoutPayment(paymentModeRaw, totalAmount, receivedAmount).second
    }
}
