package com.harrylabs.shreeshyamstore.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ShopRepository(
    private val categoryDao: CategoryDao,
    private val syncOutboxDao: SyncOutboxDao,
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao,
    private val udhaarDao: UdhaarDao,
    private val stockAdjustmentDao: StockAdjustmentDao
) {
    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun enqueueSyncOperation(
        shopId: String,
        entityType: String,
        entityUuid: String,
        createdByUid: String?,
        sourceDeviceId: String,
        operationType: String = SyncOperationType.SNAPSHOT_UPSERT
    ): String = withContext(Dispatchers.IO) {
        val operation = SyncOutboxOperation(
            shopId = shopId,
            operationType = operationType,
            entityType = entityType,
            entityUuid = entityUuid,
            sourceDeviceId = sourceDeviceId,
            createdByUid = createdByUid
        )
        syncOutboxDao.insert(operation)
        operation.localUuid
    }

    suspend fun getPendingSyncOperationCount(shopId: String): Int = withContext(Dispatchers.IO) {
        syncOutboxDao.getPendingOrFailedOperationCount(shopId)
    }

    suspend fun markPendingSyncOperationsFailed(shopId: String, lastError: String) = withContext(Dispatchers.IO) {
        syncOutboxDao.markOperationsFailed(
            shopId = shopId,
            status = SyncStatus.FAILED,
            lastError = lastError,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun clearCompletedSyncOperations(shopId: String) = withContext(Dispatchers.IO) {
        syncOutboxDao.clearPendingAndFailedOperations(shopId)
    }

    suspend fun getCategoryById(uuid: String): Category? = withContext(Dispatchers.IO) {
        categoryDao.getCategoryById(uuid)
    }
    suspend fun getCategoryByName(name: String): Category? = withContext(Dispatchers.IO) {
        categoryDao.getCategoryByName(name)
    }
    suspend fun insertCategory(category: Category): String = withContext(Dispatchers.IO) {
        categoryDao.insert(category.markPendingSync())
        category.localUuid
    }
    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.update(category.markPendingSync())
    }
    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        categoryDao.delete(
            category.copy(
                isActive = false,
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        )
    }

    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(uuid: String): Product? = withContext(Dispatchers.IO) {
        productDao.getProductById(uuid)
    }
    fun getProductByIdFlow(uuid: String): Flow<Product?> = productDao.getProductByIdFlow(uuid)
    fun getProductsByCategory(categoryUuid: String): Flow<List<Product>> = productDao.getProductsByCategory(categoryUuid)

    suspend fun insertProduct(product: Product): String = withContext(Dispatchers.IO) {
        productDao.insert(product.withLegacyFieldsSyncedToV2())
        product.localUuid
    }
    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.update(product.withLegacyFieldsSyncedToV2())
    }

    // Sales
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val allSaleItems: Flow<List<SaleItem>> = saleDao.getAllSaleItems()

    suspend fun getSaleById(uuid: String): Sale? = withContext(Dispatchers.IO) {
        saleDao.getSaleById(uuid)
    }
    fun getSaleItemsForSale(saleUuid: String): Flow<List<SaleItem>> = saleDao.getSaleItemsForSale(saleUuid)
    suspend fun getSaleItemsForSaleList(saleUuid: String): List<SaleItem> = withContext(Dispatchers.IO) {
        saleDao.getSaleItemsForSaleList(saleUuid)
    }
    fun getSalesForDateRange(start: Long, end: Long): Flow<List<Sale>> = saleDao.getSalesForDateRange(start, end)

    /**
     * Executes the major invoice lock transaction:
     * 1. Saves invoice (Sale)
     * 2. Saves line items (SaleItems)
     * 3. Subtracts stock for tracked items
     * 4. Logs a Stock Adjustment for tracking history
     * 5. Spawns an Udhaar CREDIT record if payment is selected as UPI/Cash but deferred, or specifically marked as Udhaar.
     */
    suspend fun insertSaleWithItems(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerUuid: String? = null
    ): String = withContext(Dispatchers.IO) {
        // 1. Insert Sale
        val finalCustomerId = if (sale.paymentMode == "UDHAAR") selectedCustomerUuid else null
        val finalizedSale = sale.copy(customerId = finalCustomerId)
        saleDao.insertSale(finalizedSale.withLegacyFieldsSyncedToV2())

        // 2. Loop and save each item
        for (item in items) {
            val itemToSave = item.copy(saleId = sale.localUuid)
            val product = productDao.getProductById(item.productId)
            saleDao.insertSaleItem(itemToSave.withLegacyFieldsSyncedToV2(product))

            // 3. Stock handling
            if (product != null && product.trackStock) {
                val oldStock = product.currentStock
                val oldStockBase = product.stockQuantityBase
                val soldQuantityBase = if (product.unitType == DataUnitType.PIECE) {
                    item.quantity.toLong()
                } else {
                    item.quantityBase
                }
                val newStockBase = (oldStockBase - soldQuantityBase).coerceAtLeast(0L)
                val newStock = if (product.unitType == DataUnitType.PIECE) {
                    (oldStock - item.quantity).coerceAtLeast(0)
                } else {
                    val displayBaseQty = product.priceUnitBaseQty.takeIf { it > 0L } ?: 1L
                    (newStockBase / displayBaseQty).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
                }

                // Update product stock
                val updatedProduct = product.copy(
                    currentStock = newStock,
                    stockQuantityBase = newStockBase,
                    updatedAt = System.currentTimeMillis()
                )
                productDao.update(updatedProduct.withLegacyFieldsSyncedToV2())

                // Create stock adjustment history record
                val adj = StockAdjustment(
                    productId = product.localUuid,
                    oldStock = oldStock,
                    oldQuantityBase = oldStockBase,
                    newStock = newStock,
                    newQuantityBase = newStockBase,
                    difference = newStock - oldStock,
                    differenceBase = -soldQuantityBase,
                    displayUnitSnapshot = product.displayUnit,
                    reason = "Bill Sale (No: ${sale.billNumber})",
                    createdAt = System.currentTimeMillis()
                )
                stockAdjustmentDao.insertAdjustment(adj.withLegacyFieldsSyncedToV2())
            }
        }

        // 4. Udhaar Transaction handling if payment mode is UDHAAR
        if (sale.paymentMode == "UDHAAR" && finalCustomerId != null) {
            val udhaarTx = UdhaarTransaction(
                customerId = finalCustomerId,
                saleId = sale.localUuid,
                type = "CREDIT",
                amount = sale.totalAmount,
                note = "Bill No: ${sale.billNumber}",
                createdAt = System.currentTimeMillis()
            )
            udhaarDao.insertTransaction(udhaarTx.withLegacyFieldsSyncedToV2())
        }

        sale.localUuid
    }

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun getCustomerById(uuid: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(uuid)
    }
    suspend fun getCustomerByName(name: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerByName(name)
    }
    suspend fun insertCustomer(customer: Customer): String = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer.markPendingSync())
        customer.localUuid
    }
    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer.markPendingSync())
    }
    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        customerDao.deleteCustomer(
            customer.copy(
                isActive = false,
                deletedAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        )
    }

    // Udhaar
    val allUdhaarTransactions: Flow<List<UdhaarTransaction>> = udhaarDao.getAllTransactions()

    fun getTransactionsForCustomer(customerUuid: String): Flow<List<UdhaarTransaction>> =
        udhaarDao.getTransactionsForCustomer(customerUuid)

    suspend fun getTransactionsForCustomerList(customerUuid: String): List<UdhaarTransaction> = withContext(Dispatchers.IO) {
        udhaarDao.getTransactionsForCustomerList(customerUuid)
    }

    suspend fun insertUdhaarTransaction(transaction: UdhaarTransaction): String = withContext(Dispatchers.IO) {
        udhaarDao.insertTransaction(transaction.withLegacyFieldsSyncedToV2())
        transaction.localUuid
    }

    suspend fun deleteUdhaarTransaction(transaction: UdhaarTransaction) = withContext(Dispatchers.IO) {
        udhaarDao.deleteTransaction(
            transaction.copy(
                deletedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING
            )
        )
    }

    // Stock Adjustments
    val allStockAdjustments: Flow<List<StockAdjustment>> = stockAdjustmentDao.getAllAdjustments()

    fun getAdjustmentsForProduct(productUuid: String): Flow<List<StockAdjustment>> =
        stockAdjustmentDao.getAdjustmentsForProduct(productUuid)

    suspend fun insertStockAdjustment(adjustment: StockAdjustment): String = withContext(Dispatchers.IO) {
        stockAdjustmentDao.insertAdjustment(adjustment.withLegacyFieldsSyncedToV2())
        adjustment.localUuid
    }

    /**
     * Corrects a product stock level manually and lists history
     */
    suspend fun adjustProductStock(productUuid: String, actualStockCounted: Int, reason: String) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productUuid) ?: return@withContext
        val oldStock = product.currentStock
        val diff = actualStockCounted - oldStock

        val updatedProduct = product.copy(
            currentStock = actualStockCounted,
            updatedAt = System.currentTimeMillis()
        )
        productDao.update(updatedProduct.withLegacyFieldsSyncedToV2())

        val adjustment = StockAdjustment(
            productId = productUuid,
            oldStock = oldStock,
            newStock = actualStockCounted,
            difference = diff,
            reason = reason,
            createdAt = System.currentTimeMillis()
        )
        stockAdjustmentDao.insertAdjustment(adjustment.withLegacyFieldsSyncedToV2())
    }

    suspend fun getShopDataSnapshot(): ShopDataSnapshot = withContext(Dispatchers.IO) {
        ShopDataSnapshot(
            categories = categoryDao.getAllCategoriesOnce(),
            products = productDao.getAllProductsOnce(),
            customers = customerDao.getAllCustomersOnce(),
            udhaarTransactions = udhaarDao.getAllTransactionsOnce(),
            sales = saleDao.getAllSalesOnce(),
            saleItems = saleDao.getAllSaleItemsOnce(),
            stockAdjustments = stockAdjustmentDao.getAllAdjustmentsOnce()
        )
    }

    suspend fun replaceLocalShopDataFromSnapshot(db: AppDatabase, snapshot: ShopDataSnapshot) = withContext(Dispatchers.IO) {
        db.clearAllTables()
        if (snapshot.categories.isEmpty()) {
            seedDefaultCategories()
        } else {
            categoryDao.insertAll(snapshot.categories.map { it.markRestoredFromCloud() })
        }
        productDao.insertAll(snapshot.products.map { it.markRestoredFromCloud() })
        customerDao.insertAllCustomers(snapshot.customers.map { it.markRestoredFromCloud() })
        udhaarDao.insertAllTransactions(snapshot.udhaarTransactions.map { it.markRestoredFromCloud() })
        saleDao.insertAllSales(snapshot.sales.map { it.markRestoredFromCloud() })
        saleDao.insertAllSaleItems(snapshot.saleItems.map { it.markRestoredFromCloud() })
        stockAdjustmentDao.insertAllAdjustments(snapshot.stockAdjustments.map { it.markRestoredFromCloud() })
    }

    suspend fun clearAllLocalData(db: AppDatabase) = withContext(Dispatchers.IO) {
        db.clearAllTables()
    }

    suspend fun seedDefaultCategories() = withContext(Dispatchers.IO) {
        val seededCategories = listOf(
            "Biscuits", "Cold Drinks", "Namkeen", "Dairy", "Soap/Shampoo",
            "Stationery", "Grocery", "Snacks", "Household", "Miscellaneous"
        )
        val now = System.currentTimeMillis()
        seededCategories.forEach { categoryName ->
            categoryDao.insert(
                Category(
                    name = categoryName,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }




    private fun Category.markPendingSync(): Category {
        return copy(syncStatus = SyncStatus.PENDING)
    }

    private fun Product.withLegacyFieldsSyncedToV2(): Product {
        val effectivePrice = sellingPrice?.takeIf { it > 0.0 } ?: mrp
        val purchasePricePaise = purchasePrice?.let { rupeesToPaise(it) }
        val normalizedStockBase = if (unitType == DataUnitType.PIECE) {
            currentStock.toLong()
        } else {
            stockQuantityBase
        }
        val normalizedLowStockBase = if (unitType == DataUnitType.PIECE) {
            lowStockAlertQty.toLong()
        } else {
            lowStockAlertBase
        }
        return copy(
            syncStatus = SyncStatus.PENDING,
            pricePerUnitPaise = rupeesToPaise(effectivePrice),
            priceUnitBaseQty = priceUnitBaseQty.takeIf { it > 0L } ?: 1L,
            purchasePricePerUnitPaise = purchasePricePaise,
            purchasePriceUnitBaseQty = purchasePricePaise?.let {
                purchasePriceUnitBaseQty?.takeIf { baseQty -> baseQty > 0L } ?: priceUnitBaseQty.takeIf { baseQty -> baseQty > 0L } ?: 1L
            },
            stockQuantityBase = normalizedStockBase,
            lowStockAlertBase = normalizedLowStockBase
        )
    }

    private fun Sale.withLegacyFieldsSyncedToV2(): Sale {
        return copy(
            syncStatus = SyncStatus.PENDING,
            totalAmountPaise = rupeesToPaise(totalAmount),
            saleStatus = saleStatus.ifBlank { SaleStatus.COMPLETED },
            idempotencyKey = idempotencyKey.ifBlank { newLocalUuid() }
        )
    }

    private fun SaleItem.withLegacyFieldsSyncedToV2(product: Product?): SaleItem {
        val effectivePricePaise = rupeesToPaise(unitPrice)
        val originalPricePaise = product?.pricePerUnitPaise ?: effectivePricePaise
        val priceUnitBaseQty = product?.priceUnitBaseQty?.takeIf { it > 0L } ?: 1L
        val normalizedQuantityBase = if (product?.unitType == DataUnitType.PIECE) {
            quantity.toLong()
        } else {
            quantityBase
        }
        return copy(
            syncStatus = SyncStatus.PENDING,
            unitTypeSnapshot = product?.unitType ?: unitTypeSnapshot,
            displayUnitSnapshot = product?.displayUnit ?: displayUnitSnapshot,
            baseUnitSnapshot = product?.baseUnit ?: baseUnitSnapshot,
            enteredQuantityText = enteredQuantityText.ifBlank { quantity.toString() },
            quantityBase = normalizedQuantityBase,
            originalPricePerUnitPaise = originalPricePaise,
            originalPriceUnitBaseQty = priceUnitBaseQty,
            effectivePricePerUnitPaise = effectivePricePaise,
            effectivePriceUnitBaseQty = priceUnitBaseQty,
            rateOverridden = effectivePricePaise != originalPricePaise,
            lineTotalPaise = rupeesToPaise(lineTotal),
            purchasePricePerUnitPaiseSnapshot = product?.purchasePricePerUnitPaise,
            purchasePriceUnitBaseQtySnapshot = product?.purchasePriceUnitBaseQty
        )
    }

    private fun Customer.markPendingSync(): Customer {
        return copy(syncStatus = SyncStatus.PENDING)
    }

    private fun UdhaarTransaction.withLegacyFieldsSyncedToV2(): UdhaarTransaction {
        return copy(
            syncStatus = SyncStatus.PENDING,
            amountPaise = rupeesToPaise(amount)
        )
    }

    private fun Category.markRestoredFromCloud(): Category {
        return copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis())
    }

    private fun Product.markRestoredFromCloud(): Product {
        return copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis())
    }

    private fun Customer.markRestoredFromCloud(): Customer {
        return copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis())
    }

    private fun UdhaarTransaction.markRestoredFromCloud(): UdhaarTransaction {
        return copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis())
    }

    private fun Sale.markRestoredFromCloud(): Sale {
        return copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis())
    }

    private fun SaleItem.markRestoredFromCloud(): SaleItem {
        return copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis())
    }

    private fun StockAdjustment.markRestoredFromCloud(): StockAdjustment {
        return copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis())
    }

    private fun StockAdjustment.withLegacyFieldsSyncedToV2(): StockAdjustment {
        return copy(syncStatus = SyncStatus.PENDING)
    }
}
