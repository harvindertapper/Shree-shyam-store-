package com.aistudio.shreeshyamstore.pqwzkb.commerce

import java.util.Locale

/** Pure validation shared by UI, repository writes, and future Control Plane commands. */
object InventoryValidation {
    fun normalizeName(value: String): String = value.trim().replace(Regex("\\s+"), " ")

    fun normalizeBarcode(value: String): String? = value
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.uppercase(Locale.ENGLISH)

    fun normalizeUnit(value: String): String = normalizeName(value).ifEmpty { "pcs" }

    fun validateCategoryName(value: String): String {
        val normalized = normalizeName(value)
        require(normalized.isNotEmpty()) { "Category name is required" }
        require(normalized.length <= 80) { "Category name is too long" }
        return normalized
    }

    fun validateProductName(value: String): String {
        val normalized = normalizeName(value)
        require(normalized.isNotEmpty()) { "Product name is required" }
        require(normalized.length <= 160) { "Product name is too long" }
        return normalized
    }

    fun validateProductMoney(value: Long, field: String): Long {
        require(value >= 0L) { "$field cannot be negative" }
        return value
    }

    fun validateOptionalMoney(value: Long?, field: String): Long? {
        if (value != null) validateProductMoney(value, field)
        return value
    }

    fun validateQuantity(value: Double, field: String): Double {
        require(value.isFinite() && value >= 0.0) { "$field must be finite and non-negative" }
        return value
    }

    fun validateUnit(value: String): String {
        val normalized = normalizeUnit(value)
        require(normalized.length <= 24) { "Unit is too long" }
        return normalized
    }

    fun validateReason(value: String): String = normalizeName(value).ifEmpty {
        "Manual stock adjustment"
    }

    fun isMarketplaceEligible(
        isActive: Boolean,
        name: String,
        categoryId: Long,
        mrp: Long,
        unit: String
    ): Boolean = isActive &&
        normalizeName(name).isNotEmpty() &&
        categoryId > 0L &&
        mrp >= 0L &&
        normalizeName(unit).isNotEmpty()
}
