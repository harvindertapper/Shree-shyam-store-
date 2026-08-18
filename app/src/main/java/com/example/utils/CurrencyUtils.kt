package com.example.utils

import java.util.Locale

object CurrencyUtils {
    fun formatRupees(amountMinorUnits: Long): String = MoneyUtils.formatRupees(amountMinorUnits)

    fun formatQuantity(qty: Double, unit: String = "Pcs"): String {
        return if (qty % 1.0 == 0.0) {
            "${qty.toInt()} $unit"
        } else {
            String.format(Locale.US, "%.3f %s", qty, unit).trimEnd('0').trimEnd('.') + " $unit"
        }
    }
}
