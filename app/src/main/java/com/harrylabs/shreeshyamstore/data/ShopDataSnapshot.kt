package com.harrylabs.shreeshyamstore.data

data class ShopDataSnapshot(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val udhaarTransactions: List<UdhaarTransaction> = emptyList(),
    val sales: List<Sale> = emptyList(),
    val saleItems: List<SaleItem> = emptyList(),
    val stockAdjustments: List<StockAdjustment> = emptyList()
)
