package com.harrylabs.shreeshyamstore.utils

object UdhaarReminderFormatter {
    fun buildReminderMessage(
        shopName: String,
        customerName: String,
        balanceRupees: Double,
        paymentNote: String? = null
    ): String {
        val cleanShopName = shopName.trim().ifBlank { "Shree Shyam Store" }
        val cleanCustomerName = customerName.trim().ifBlank { "Customer" }
        val note = paymentNote?.trim().orEmpty()

        return buildString {
            append("Namaste ").append(cleanCustomerName).append(",\n")
            append(cleanShopName)
                .append(" ke hisaab se aapka udhaar balance ")
                .append(CurrencyUtils.formatRupees(balanceRupees))
                .append(" hai.")
            if (note.isNotBlank()) {
                append("\n").append(note)
            }
            append("\n\nDhanyavaad,\n").append(cleanShopName)
        }
    }
}
