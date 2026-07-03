package com.harrylabs.shreeshyamstore.utils

import com.harrylabs.shreeshyamstore.data.Category
import com.harrylabs.shreeshyamstore.data.DataDisplayUnit
import com.harrylabs.shreeshyamstore.data.DataUnitType
import com.harrylabs.shreeshyamstore.data.Product
import com.harrylabs.shreeshyamstore.data.Sale
import com.harrylabs.shreeshyamstore.data.SaleItem
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnerInsightsCalculatorTest {
    @Test
    fun stockValueUsesTrackedBaseUnitsAndGroupsByCategory() {
        val grocery = Category(localUuid = "cat-grocery", name = "Grocery")
        val sugar = Product(
            localUuid = "prod-sugar",
            name = "Sugar",
            categoryId = grocery.localUuid,
            mrp = 47.0,
            sellingPrice = 47.0,
            purchasePrice = 40.0,
            unitType = DataUnitType.WEIGHT,
            displayUnit = DataDisplayUnit.KILOGRAM,
            baseUnit = DataDisplayUnit.GRAM,
            pricePerUnitPaise = 4_700,
            priceUnitBaseQty = 1_000,
            purchasePricePerUnitPaise = 4_000,
            purchasePriceUnitBaseQty = 1_000,
            stockQuantityBase = 2_500,
            currentStock = 2,
            trackStock = true
        )
        val untrackedToffee = Product(
            localUuid = "prod-toffee",
            name = "Toffee",
            categoryId = grocery.localUuid,
            mrp = 1.0,
            sellingPrice = 1.0,
            stockQuantityBase = 1_000,
            currentStock = 1_000,
            trackStock = false
        )

        val summary = OwnerInsightsCalculator.stockValue(
            products = listOf(sugar, untrackedToffee),
            categories = listOf(grocery)
        )

        assertEquals(11_750L, summary.totalSellingValuePaise)
        assertEquals(10_000L, summary.totalPurchaseValuePaise)
        assertEquals(1_750L, summary.potentialMarginPaise)
        assertEquals(1, summary.trackedProductCount)
        assertEquals(1, summary.untrackedProductCount)
        assertEquals(11_750L, summary.categoryValues.single().sellingValuePaise)
        assertEquals("Grocery", summary.categoryValues.single().categoryName)
    }

    @Test
    fun profitUsesSaleItemPurchaseCostSnapshotsNotCurrentProductCost() {
        val now = 1_725_000_000_000L
        val todayStart = now - 1_000L
        val todayEnd = now + 1_000L
        val monthStart = now - 10_000L
        val monthEnd = now + 10_000L
        val sale = Sale(
            localUuid = "sale-1",
            billNumber = "BILL-1",
            totalAmount = 50.0,
            paymentMode = "CASH",
            createdAt = now
        )
        val saleItem = SaleItem(
            saleId = sale.localUuid,
            productId = "prod-sugar",
            productNameSnapshot = "Sugar",
            quantity = 1,
            unitTypeSnapshot = DataUnitType.WEIGHT,
            displayUnitSnapshot = DataDisplayUnit.KILOGRAM,
            baseUnitSnapshot = DataDisplayUnit.GRAM,
            enteredQuantityText = "1.064 kg",
            quantityBase = 1_064,
            unitPrice = 50.0,
            lineTotal = 50.0,
            lineTotalPaise = 5_000,
            purchasePricePerUnitPaiseSnapshot = 4_000,
            purchasePriceUnitBaseQtySnapshot = 1_000
        )

        val summary = OwnerInsightsCalculator.profit(
            sales = listOf(sale),
            saleItems = listOf(saleItem),
            todayStart = todayStart,
            todayEnd = todayEnd,
            monthStart = monthStart,
            monthEnd = monthEnd
        )

        assertEquals(744L, summary.todayProfitPaise)
        assertEquals(744L, summary.monthProfitPaise)
        assertEquals(4_256L, summary.todayPurchaseCostPaise)
        assertEquals(0, summary.missingPurchaseCostLineCount)
    }
}
