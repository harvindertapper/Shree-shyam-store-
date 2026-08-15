package com.example.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatRupees(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
            var formatted = format.format(amount)
            // Replace INR or Rs. with the classic ₹ symbol
            formatted = formatted
                .replace("Rs.", "₹")
                .replace("INR", "₹")
                .replace("Rs", "₹")
                .trim()
            if (!formatted.contains("₹")) {
                "₹$formatted"
            } else {
                formatted
            }
        } catch (e: Exception) {
            String.format(Locale.US, "₹%.2f", amount)
        }
    }

    fun formatQuantity(qty: Double, unit: String = "Pcs"): String {
        return if (qty % 1.0 == 0.0) {
            "${qty.toInt()} $unit"
        } else {
            String.format(Locale.US, "%.3f %s", qty, unit).trimEnd('0').trimEnd('.') + " $unit"
        }
    }
}
