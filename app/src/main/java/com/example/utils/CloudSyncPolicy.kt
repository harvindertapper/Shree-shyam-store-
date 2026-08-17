package com.example.utils

/**
 * Defines the tables that are safe to replicate as shop business data.
 * User accounts and credential material are device-local and intentionally absent.
 */
internal object CloudSyncPolicy {
    private val cloudBusinessTables = setOf(
        "categories",
        "products",
        "sales",
        "sale_items",
        "customers",
        "udhaar_transactions",
        "stock_adjustments"
    )

    fun isCloudBusinessTable(tableName: String): Boolean =
        tableName.trim() in cloudBusinessTables
}
