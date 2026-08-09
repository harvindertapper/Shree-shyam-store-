package com.harrylabs.shreeshyamstore.utils

import com.harrylabs.shreeshyamstore.data.Category
import com.harrylabs.shreeshyamstore.data.Product
import com.harrylabs.shreeshyamstore.data.Sale
import com.harrylabs.shreeshyamstore.data.SaleItem
import java.math.BigInteger

data class CategoryStockValue(
    val categoryId: String,
    val categoryName: String,
    val sellingValuePaise: Long,
    val purchaseValuePaise: Long,
    val potentialMarginPaise: Long,
    val trackedProductCount: Int
)

data class StockValueSummary(
    val totalSellingValuePaise: Long,
    val totalPurchaseValuePaise: Long,
    val potentialMarginPaise: Long,
    val categoryValues: List<CategoryStockValue>,
    val trackedProductCount: Int,
    val untrackedProductCount: Int,
    val missingPurchasePriceProductCount: Int
)

data class ProfitSummary(
    val todayProfitPaise: Long,
    val monthProfitPaise: Long,
    val todaySalesValuePaise: Long,
    val todayPurchaseCostPaise: Long,
    val monthSalesValuePaise: Long,
    val monthPurchaseCostPaise: Long,
    val missingPurchaseCostLineCount: Int
)

object OwnerInsightsCalculator {
    fun stockValue(
        products: List<Product>,
        categories: List<Category>
    ): StockValueSummary {
        val categoryNameById = categories.associate { it.localUuid to it.name }
        val activeProducts = products.filter { it.isActive && it.deletedAt == null }
        val trackedProducts = activeProducts.filter { it.trackStock && it.stockQuantityBase > 0L }
        val categoryValues = trackedProducts
            .groupBy { it.categoryId }
            .map { (categoryId, groupedProducts) ->
                val sellingValue = groupedProducts.sumOf { product ->
                    valueForStock(
                        stockQuantityBase = product.stockQuantityBase,
                        pricePerUnitPaise = product.pricePerUnitPaise,
                        priceUnitBaseQty = product.priceUnitBaseQty
                    )
                }
                val purchaseValue = groupedProducts.sumOf { product ->
                    val purchasePrice = product.purchasePricePerUnitPaise
                    val purchaseBaseQty = product.purchasePriceUnitBaseQty
                    if (purchasePrice != null && purchaseBaseQty != null) {
                        valueForStock(
                            stockQuantityBase = product.stockQuantityBase,
                            pricePerUnitPaise = purchasePrice,
                            priceUnitBaseQty = purchaseBaseQty
                        )
                    } else {
                        0L
                    }
                }
                CategoryStockValue(
                    categoryId = categoryId,
                    categoryName = categoryNameById[categoryId] ?: "Uncategorized",
                    sellingValuePaise = sellingValue,
                    purchaseValuePaise = purchaseValue,
                    potentialMarginPaise = sellingValue - purchaseValue,
                    trackedProductCount = groupedProducts.size
                )
            }
            .sortedByDescending { it.sellingValuePaise }

        val totalSellingValue = categoryValues.sumOf { it.sellingValuePaise }
        val totalPurchaseValue = categoryValues.sumOf { it.purchaseValuePaise }

        return StockValueSummary(
            totalSellingValuePaise = totalSellingValue,
            totalPurchaseValuePaise = totalPurchaseValue,
            potentialMarginPaise = totalSellingValue - totalPurchaseValue,
            categoryValues = categoryValues,
            trackedProductCount = trackedProducts.size,
            untrackedProductCount = activeProducts.count { !it.trackStock },
            missingPurchasePriceProductCount = trackedProducts.count {
                it.purchasePricePerUnitPaise == null || it.purchasePriceUnitBaseQty == null
            }
        )
    }

    fun profit(
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        todayStart: Long,
        todayEnd: Long,
        monthStart: Long,
        monthEnd: Long
    ): ProfitSummary {
        val saleById = sales
            .filter { it.deletedAt == null }
            .associateBy { it.localUuid }
        var todaySalesValue = 0L
        var todayPurchaseCost = 0L
        var monthSalesValue = 0L
        var monthPurchaseCost = 0L
        var missingPurchaseCostLines = 0

        saleItems
            .filter { it.deletedAt == null }
            .forEach { item ->
                val sale = saleById[item.saleId] ?: return@forEach
                val purchaseCost = purchaseCostSnapshot(item)
                if (purchaseCost == null) {
                    missingPurchaseCostLines += 1
                    return@forEach
                }
                if (sale.createdAt in todayStart..todayEnd) {
                    todaySalesValue += item.lineTotalPaise
                    todayPurchaseCost += purchaseCost
                }
                if (sale.createdAt in monthStart..monthEnd) {
                    monthSalesValue += item.lineTotalPaise
                    monthPurchaseCost += purchaseCost
                }
            }

        return ProfitSummary(
            todayProfitPaise = todaySalesValue - todayPurchaseCost,
            monthProfitPaise = monthSalesValue - monthPurchaseCost,
            todaySalesValuePaise = todaySalesValue,
            todayPurchaseCostPaise = todayPurchaseCost,
            monthSalesValuePaise = monthSalesValue,
            monthPurchaseCostPaise = monthPurchaseCost,
            missingPurchaseCostLineCount = missingPurchaseCostLines
        )
    }

    private fun valueForStock(
        stockQuantityBase: Long,
        pricePerUnitPaise: Long,
        priceUnitBaseQty: Long
    ): Long {
        if (stockQuantityBase <= 0L || pricePerUnitPaise <= 0L || priceUnitBaseQty <= 0L) {
            return 0L
        }
        return multiplyDivideHalfUp(
            multiplicand = stockQuantityBase,
            multiplier = pricePerUnitPaise,
            divisor = priceUnitBaseQty
        )
    }

    private fun purchaseCostSnapshot(item: SaleItem): Long? {
        val purchasePrice = item.purchasePricePerUnitPaiseSnapshot ?: return null
        val purchaseBaseQty = item.purchasePriceUnitBaseQtySnapshot ?: return null
        if (purchasePrice <= 0L || purchaseBaseQty <= 0L || item.quantityBase <= 0L) return null
        return multiplyDivideHalfUp(
            multiplicand = item.quantityBase,
            multiplier = purchasePrice,
            divisor = purchaseBaseQty
        )
    }

    private fun multiplyDivideHalfUp(multiplicand: Long, multiplier: Long, divisor: Long): Long {
        val product = BigInteger.valueOf(multiplicand).multiply(BigInteger.valueOf(multiplier))
        val divisorValue = BigInteger.valueOf(divisor)
        val quotientAndRemainder = product.divideAndRemainder(divisorValue)
        val doubledRemainder = quotientAndRemainder[1].multiply(BigInteger.valueOf(2))
        val rounded = if (doubledRemainder >= divisorValue) {
            quotientAndRemainder[0] + BigInteger.ONE
        } else {
            quotientAndRemainder[0]
        }
        return rounded.longValueExact()
    }
}
