package com.aistudio.shreeshyamstore.pqwzkb.commerce

import com.aistudio.shreeshyamstore.pqwzkb.utils.MoneyUtils
import java.math.BigDecimal
import java.util.Locale

enum class ProductFormField {
    NAME,
    CATEGORY,
    MRP,
    SELLING_PRICE,
    PURCHASE_PRICE,
    UNIT,
    STOCK,
    LOW_STOCK_ALERT,
    BARCODE
}

enum class ProductFormError {
    REQUIRED,
    INVALID,
    NEGATIVE,
    NON_POSITIVE,
    FRACTION_NOT_ALLOWED,
    INVALID_BARCODE,
    MISSING_CATEGORY
}

data class ProductFormValidationResult(
    val normalizedName: String,
    val normalizedCategoryId: Long?,
    val normalizedMrp: Long?,
    val normalizedSellingPrice: Long?,
    val normalizedPurchasePrice: Long?,
    val normalizedUnit: String,
    val normalizedStock: Double?,
    val normalizedLowStockAlert: Double?,
    val normalizedBarcode: String?,
    val errors: Map<ProductFormField, ProductFormError>
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}

/** Pure form policy shared by Compose and future non-Android catalog clients. */
object ProductFormValidation {
    private val DECIMAL_SEPARATOR_PATTERN = Regex("^[+-]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)$")

    fun parseQuantityInput(value: String): Double? {
        val normalized = value.trim().replace(',', '.')
        if (normalized.isEmpty() || !DECIMAL_SEPARATOR_PATTERN.matches(normalized)) return null
        return runCatching {
            BigDecimal(normalized).toDouble().also { require(it.isFinite()) }
        }.getOrNull()
    }

    fun quantityError(value: String, unit: String): ProductFormError? {
        val normalized = value.trim()
        if (normalized.isEmpty()) return ProductFormError.REQUIRED
        if (normalized.startsWith('-')) return ProductFormError.NEGATIVE
        val parsed = parseQuantityInput(normalized) ?: return ProductFormError.INVALID
        if (parsed < 0.0) return ProductFormError.NEGATIVE
        if (InventoryValidation.isWholeQuantityUnit(unit) && parsed % 1.0 != 0.0) {
            return ProductFormError.FRACTION_NOT_ALLOWED
        }
        return null
    }

    fun validate(
        name: String,
        categoryId: Long?,
        mrp: String,
        sellingPrice: String,
        purchasePrice: String,
        unit: String,
        stock: String,
        lowStockAlert: String,
        barcode: String,
        trackStock: Boolean
    ): ProductFormValidationResult {
        val errors = linkedMapOf<ProductFormField, ProductFormError>()
        val normalizedName = InventoryValidation.normalizeName(name)
        when {
            normalizedName.isEmpty() -> errors[ProductFormField.NAME] = ProductFormError.REQUIRED
            normalizedName.length > 160 -> errors[ProductFormField.NAME] = ProductFormError.INVALID
        }

        if (categoryId == null || categoryId <= 0L) {
            errors[ProductFormField.CATEGORY] = ProductFormError.MISSING_CATEGORY
        }

        val normalizedMrp = parseMoney(
            input = mrp,
            required = true,
            strictlyPositive = true,
            field = ProductFormField.MRP,
            errors = errors
        )
        val normalizedSellingPrice = parseMoney(
            input = sellingPrice,
            required = false,
            strictlyPositive = false,
            field = ProductFormField.SELLING_PRICE,
            errors = errors
        )
        val normalizedPurchasePrice = parseMoney(
            input = purchasePrice,
            required = false,
            strictlyPositive = false,
            field = ProductFormField.PURCHASE_PRICE,
            errors = errors
        )

        val normalizedUnit = InventoryValidation.normalizeName(unit)
        when {
            normalizedUnit.isEmpty() -> errors[ProductFormField.UNIT] = ProductFormError.REQUIRED
            normalizedUnit.length > 24 -> errors[ProductFormField.UNIT] = ProductFormError.INVALID
        }

        val normalizedStock = if (trackStock) {
            parseQuantity(
                input = stock,
                unit = normalizedUnit,
                field = ProductFormField.STOCK,
                errors = errors
            )
        } else {
            null
        }
        val normalizedLowStockAlert = if (trackStock) {
            parseQuantity(
                input = lowStockAlert,
                unit = normalizedUnit,
                field = ProductFormField.LOW_STOCK_ALERT,
                errors = errors
            )
        } else {
            null
        }

        val normalizedBarcode = if (barcode.trim().isEmpty()) {
            null
        } else {
            runCatching { InventoryValidation.validateOptionalBarcode(barcode) }
                .onFailure { errors[ProductFormField.BARCODE] = ProductFormError.INVALID_BARCODE }
                .getOrNull()
        }

        return ProductFormValidationResult(
            normalizedName = normalizedName,
            normalizedCategoryId = categoryId?.takeIf { it > 0L },
            normalizedMrp = normalizedMrp,
            normalizedSellingPrice = normalizedSellingPrice,
            normalizedPurchasePrice = normalizedPurchasePrice,
            normalizedUnit = normalizedUnit,
            normalizedStock = normalizedStock,
            normalizedLowStockAlert = normalizedLowStockAlert,
            normalizedBarcode = normalizedBarcode,
            errors = errors
        )
    }

    private fun parseMoney(
        input: String,
        required: Boolean,
        strictlyPositive: Boolean,
        field: ProductFormField,
        errors: MutableMap<ProductFormField, ProductFormError>
    ): Long? {
        val normalized = input.trim()
        if (normalized.isEmpty()) {
            if (required) errors[field] = ProductFormError.REQUIRED
            return null
        }
        if (normalized.startsWith('-')) {
            errors[field] = ProductFormError.NEGATIVE
            return null
        }
        val parsed = MoneyUtils.parseMajorUnits(normalized)
        if (parsed == null) {
            errors[field] = ProductFormError.INVALID
            return null
        }
        if (strictlyPositive && parsed <= 0L) {
            errors[field] = ProductFormError.NON_POSITIVE
            return parsed
        }
        return parsed
    }

    private fun parseQuantity(
        input: String,
        unit: String,
        field: ProductFormField,
        errors: MutableMap<ProductFormField, ProductFormError>
    ): Double? {
        val normalized = input.trim()
        if (normalized.isEmpty()) {
            errors[field] = ProductFormError.REQUIRED
            return null
        }
        if (normalized.startsWith('-')) {
            errors[field] = ProductFormError.NEGATIVE
            return null
        }
        val parsed = parseQuantityInput(normalized)
        if (parsed == null) {
            errors[field] = ProductFormError.INVALID
            return null
        }
        if (parsed < 0.0) {
            errors[field] = ProductFormError.NEGATIVE
            return null
        }
        if (InventoryValidation.isWholeQuantityUnit(unit) && parsed % 1.0 != 0.0) {
            errors[field] = ProductFormError.FRACTION_NOT_ALLOWED
        }
        return parsed
    }

    fun decimalSeparatorDescription(languageIsHindi: Boolean): String =
        if (languageIsHindi) "दशमलव के लिए 1,5 या 1.5 लिख सकते हैं।" else "For decimals, enter 1.5 or 1,5."

    fun isCommaDecimal(value: String): Boolean = value.contains(',') && !value.contains('.')

    fun normalizedUnitKey(value: String): String = InventoryValidation.normalizeName(value).lowercase(Locale.ENGLISH)
}
