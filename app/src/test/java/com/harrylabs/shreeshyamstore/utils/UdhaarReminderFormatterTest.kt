package com.harrylabs.shreeshyamstore.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class UdhaarReminderFormatterTest {
    @Test
    fun reminderMessageContainsShopCustomerAndBalance() {
        val message = UdhaarReminderFormatter.buildReminderMessage(
            shopName = "Shree Shyam Store",
            customerName = "Ramesh",
            balanceRupees = 1250.50,
            paymentNote = "Please pay when convenient."
        )

        assertTrue(message.contains("Shree Shyam Store"))
        assertTrue(message.contains("Ramesh"))
        assertTrue(message.contains("₹1,250.50"))
        assertTrue(message.contains("Please pay when convenient."))
    }
}
