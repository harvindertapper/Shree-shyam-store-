package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.InventoryValidation
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ProductFormError
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ProductFormField
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ProductFormValidation
import com.aistudio.shreeshyamstore.pqwzkb.utils.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryCatalogTest {
    @Test
    fun namesAndUnitsCollapseWhitespaceWithoutChangingMeaning() {
        assertEquals("Aata 10 kg", InventoryValidation.normalizeName("  Aata   10 kg  "))
        assertEquals("pcs", InventoryValidation.validateUnit("   "))
        assertEquals("box pack", InventoryValidation.validateUnit(" box   pack "))
    }

    @Test
    fun barcodeIdentityIsOptionalButCanonicalWhenPresent() {
        assertNull(InventoryValidation.normalizeBarcode("  "))
        assertEquals("AB-123", InventoryValidation.normalizeBarcode("  ab-123  "))
    }

    @Test
    fun moneyAndQuantityValidationRejectInvalidValues() {
        assertEquals(1250L, InventoryValidation.validateProductMoney(1250L, "MRP"))
        assertTrue(runCatching { InventoryValidation.validateProductMoney(-1L, "MRP") }.isFailure)
        assertTrue(runCatching { InventoryValidation.validateQuantity(Double.NaN, "Stock") }.isFailure)
        assertTrue(runCatching { InventoryValidation.validateQuantity(-0.01, "Stock") }.isFailure)
        assertTrue(runCatching { InventoryValidation.validateQuantity(2.5, "Stock") }.isSuccess)
    }

    @Test
    fun productFormRejectsMalformedAndMissingValuesWithoutDefaulting() {
        val result = ProductFormValidation.validate(
            name = "Rice",
            categoryId = null,
            mrp = "not-a-number",
            sellingPrice = "",
            purchasePrice = "-2",
            unit = "pcs",
            stock = "",
            lowStockAlert = "1.5",
            barcode = "BAD CODE",
            trackStock = true
        )

        assertEquals(ProductFormError.MISSING_CATEGORY, result.errors[ProductFormField.CATEGORY])
        assertEquals(ProductFormError.INVALID, result.errors[ProductFormField.MRP])
        assertEquals(ProductFormError.NEGATIVE, result.errors[ProductFormField.PURCHASE_PRICE])
        assertEquals(ProductFormError.REQUIRED, result.errors[ProductFormField.STOCK])
        assertEquals(ProductFormError.FRACTION_NOT_ALLOWED, result.errors[ProductFormField.LOW_STOCK_ALERT])
        assertEquals(ProductFormError.INVALID_BARCODE, result.errors[ProductFormField.BARCODE])
        assertFalse(result.isValid)
    }

    @Test
    fun productFormAcceptsCommaDecimalsForMoneyAndFractionalWeight() {
        val result = ProductFormValidation.validate(
            name = "Rice",
            categoryId = 7L,
            mrp = "12,50",
            sellingPrice = "11.25",
            purchasePrice = "10,00",
            unit = "kg",
            stock = "2,5",
            lowStockAlert = "0.5",
            barcode = "ab-123",
            trackStock = true
        )

        assertTrue(result.isValid)
        assertEquals(1250L, result.normalizedMrp)
        assertEquals(1125L, result.normalizedSellingPrice)
        assertEquals(1000L, result.normalizedPurchasePrice)
        assertEquals(2.5, result.normalizedStock!!, 0.0)
        assertEquals("AB-123", result.normalizedBarcode)
        assertEquals(1250L, MoneyUtils.parseMajorUnits("12,5"))
        assertNull(MoneyUtils.parseMajorUnits("1e3"))
        assertNull(MoneyUtils.parseMajorUnits("1,234.56"))
    }

    @Test
    fun productFormRejectsZeroMrpAndFractionalPieceQuantities() {
        val result = ProductFormValidation.validate(
            name = "Soap",
            categoryId = 1L,
            mrp = "0",
            sellingPrice = "0",
            purchasePrice = "0",
            unit = "pcs",
            stock = "1.25",
            lowStockAlert = "0",
            barcode = "",
            trackStock = true
        )

        assertEquals(ProductFormError.NON_POSITIVE, result.errors[ProductFormField.MRP])
        assertEquals(ProductFormError.FRACTION_NOT_ALLOWED, result.errors[ProductFormField.STOCK])
        assertFalse(result.isValid)
    }

    @Test
    fun repositoryQuantityPolicyAllowsFractionalWeightButRequiresWholePieces() {
        assertTrue(runCatching {
            InventoryValidation.validateQuantityForUnit(1.5, "Stock", "kg")
        }.isSuccess)
        assertTrue(runCatching {
            InventoryValidation.validateQuantityForUnit(1.5, "Stock", "pcs")
        }.isFailure)
        assertTrue(runCatching {
            InventoryValidation.validateRequiredUnit("   ")
        }.isFailure)
        assertTrue(runCatching {
            InventoryValidation.validateOptionalBarcode("BAD CODE")
        }.isFailure)
    }

    @Test
    fun marketplaceEligibilityRequiresAnActiveCompleteCatalogRecord() {
        assertTrue(InventoryValidation.isMarketplaceEligible(true, " Rice ", 2L, 5000L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(false, "Rice", 2L, 5000L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(true, "Rice", 0L, 5000L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(true, "Rice", 2L, -1L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(true, "Rice", 2L, 0L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(true, "", 2L, 5000L, "kg"))
    }
}
