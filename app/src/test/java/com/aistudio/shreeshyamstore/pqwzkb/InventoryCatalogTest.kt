package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.commerce.InventoryValidation
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
    fun marketplaceEligibilityRequiresAnActiveCompleteCatalogRecord() {
        assertTrue(InventoryValidation.isMarketplaceEligible(true, " Rice ", 2L, 5000L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(false, "Rice", 2L, 5000L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(true, "Rice", 0L, 5000L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(true, "Rice", 2L, -1L, "kg"))
        assertFalse(InventoryValidation.isMarketplaceEligible(true, "", 2L, 5000L, "kg"))
    }
}
