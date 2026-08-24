package com.aistudio.shreeshyamstore.pqwzkb.commerce

import java.util.Locale

/** Pure validation shared by UI, repository writes, and future Control Plane commands. */
object InventoryValidation {
    fun normalizeName(value: String): String = value.trim().replace(Regex("\\s+"), " ")

    fun normalizeBarcode(value: String): String? = value
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.uppercase(Locale.ENGLISH)

    fun validateOptionalBarcode(value: String): String? {
        val normalized = value.trim()
        if (normalized.isEmpty()) return null
        require(normalized.length <= 128) { "Barcode is too long" }
        require(normalized.none { it.isWhitespace() || it.code < 0x20 || it.code == 0x7f }) {
            "Barcode cannot contain spaces or control characters"
        }
        return normalized.uppercase(Locale.ENGLISH)
    }

    fun normalizeUnit(value: String): String = normalizeName(value).ifEmpty { "pcs" }

    fun validateRequiredUnit(value: String): String {
        val normalized = normalizeName(value)
        require(normalized.isNotEmpty()) { "Unit is required" }
        require(normalized.length <= 24) { "Unit is too long" }
        return normalized
    }

    private val DECIMAL_QUANTITY_UNITS = setOf(
        "kg", "kilogram", "kilograms", "g", "gram", "grams", "mg", "milligram", "milligrams",
        "l", "litre", "litres", "liter", "liters", "ml", "millilitre", "millilitres",
        "milliliter", "milliliters", "m", "metre", "metres", "meter", "meters", "cm", "centimetre", "centimeters"
    )

    fun isWholeQuantityUnit(value: String): Boolean {
        val normalized = normalizeName(value).lowercase(Locale.ENGLISH)
        return normalized.isNotEmpty() && normalized !in DECIMAL_QUANTITY_UNITS
    }

    fun validateQuantityForUnit(value: Double, field: String, unit: String): Double {
        val normalized = validateQuantity(value, field)
        require(!isWholeQuantityUnit(unit) || normalized % 1.0 == 0.0) {
            "$field must be a whole number for $unit"
        }
        return normalized
    }

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

    fun validateRequiredProductMoney(value: Long, field: String): Long {
        require(value > 0L) { "$field must be greater than zero" }
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
        mrp > 0L &&
        normalizeName(unit).isNotEmpty()
}
