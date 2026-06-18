package com.harrylabs.shreeshyamstore.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatRupees(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
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
            String.format("₹%.2f", amount)
        }
    }
}
