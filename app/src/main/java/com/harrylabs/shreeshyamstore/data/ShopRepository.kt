package com.harrylabs.shreeshyamstore.data

import kotlinx.coroutines.flow.Flow

class ShopRepository(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao,
    private val udhaarDao: UdhaarDao,
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val userDao: UserDao
) {
    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(uuid: String): Category? = categoryDao.getCategoryById(uuid)
    suspend fun getCategoryByName(name: String): Category? = categoryDao.getCategoryByName(name)
    suspend fun insertCategory(category: Category): String {
        categoryDao.insert(category.markPendingSync())
        return category.localUuid
    }
    suspend fun updateCategory(category: Category) = categoryDao.update(category.markPendingSync())
    suspend fun deleteCategory(category: Category) {
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

    suspend fun getProductById(uuid: String): Product? = productDao.getProductById(uuid)
    fun getProductByIdFlow(uuid: String): Flow<Product?> = productDao.getProductByIdFlow(uuid)
    fun getProductsByCategory(categoryUuid: String): Flow<List<Product>> = productDao.getProductsByCategory(categoryUuid)

    suspend fun insertProduct(product: Product): String {
        productDao.insert(product.withLegacyFieldsSyncedToV2())
        return product.localUuid
    }
    suspend fun updateProduct(product: Product) = productDao.update(product.withLegacyFieldsSyncedToV2())

    // Sales
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()

    suspend fun getSaleById(uuid: String): Sale? = saleDao.getSaleById(uuid)
    fun getSaleItemsForSale(saleUuid: String): Flow<List<SaleItem>> = saleDao.getSaleItemsForSale(saleUuid)
    suspend fun getSaleItemsForSaleList(saleUuid: String): List<SaleItem> = saleDao.getSaleItemsForSaleList(saleUuid)
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
    ): String {
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
                val newStock = oldStock - item.quantity

                // Update product stock
                val updatedProduct = product.copy(
                    currentStock = newStock,
                    updatedAt = System.currentTimeMillis()
                )
                productDao.update(updatedProduct.withLegacyFieldsSyncedToV2())

                // Create stock adjustment history record
                val adj = StockAdjustment(
                    productId = product.localUuid,
                    oldStock = oldStock,
                    newStock = newStock,
                    difference = -item.quantity,
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

        return sale.localUuid
    }

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()

    suspend fun getCustomerById(uuid: String): Customer? = customerDao.getCustomerById(uuid)
    suspend fun getCustomerByName(name: String): Customer? = customerDao.getCustomerByName(name)
    suspend fun insertCustomer(customer: Customer): String {
        customerDao.insertCustomer(customer.markPendingSync())
        return customer.localUuid
    }
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer.markPendingSync())
    suspend fun deleteCustomer(customer: Customer) {
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

    suspend fun getTransactionsForCustomerList(customerUuid: String): List<UdhaarTransaction> =
        udhaarDao.getTransactionsForCustomerList(customerUuid)

    suspend fun insertUdhaarTransaction(transaction: UdhaarTransaction): String {
        udhaarDao.insertTransaction(transaction.withLegacyFieldsSyncedToV2())
        return transaction.localUuid
    }

    suspend fun deleteUdhaarTransaction(transaction: UdhaarTransaction) {
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

    suspend fun insertStockAdjustment(adjustment: StockAdjustment): String {
        stockAdjustmentDao.insertAdjustment(adjustment.withLegacyFieldsSyncedToV2())
        return adjustment.localUuid
    }

    /**
     * Corrects a product stock level manually and lists history
     */
    suspend fun adjustProductStock(productUuid: String, actualStockCounted: Int, reason: String) {
        val product = productDao.getProductById(productUuid) ?: return
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

    // --- User Authentication / Session Management Functions ---
    suspend fun getUserByUsernameOrEmail(username: String, email: String): User? = userDao.getUserByUsernameOrEmail(username, email)
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)

    private fun Category.markPendingSync(): Category {
        return copy(syncStatus = SyncStatus.PENDING)
    }

    private fun Product.withLegacyFieldsSyncedToV2(): Product {
        val effectivePrice = sellingPrice?.takeIf { it > 0.0 } ?: mrp
        val purchasePricePaise = purchasePrice?.let { rupeesToPaise(it) }
        return copy(
            syncStatus = SyncStatus.PENDING,
            pricePerUnitPaise = rupeesToPaise(effectivePrice),
            priceUnitBaseQty = priceUnitBaseQty.takeIf { it > 0L } ?: 1L,
            purchasePricePerUnitPaise = purchasePricePaise,
            purchasePriceUnitBaseQty = purchasePricePaise?.let {
                purchasePriceUnitBaseQty?.takeIf { baseQty -> baseQty > 0L } ?: 1L
            },
            stockQuantityBase = currentStock.toLong(),
            lowStockAlertBase = lowStockAlertQty.toLong()
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
        return copy(
            syncStatus = SyncStatus.PENDING,
            unitTypeSnapshot = product?.unitType ?: unitTypeSnapshot,
            displayUnitSnapshot = product?.displayUnit ?: displayUnitSnapshot,
            baseUnitSnapshot = product?.baseUnit ?: baseUnitSnapshot,
            enteredQuantityText = quantity.toString(),
            quantityBase = quantity.toLong(),
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

    private fun StockAdjustment.withLegacyFieldsSyncedToV2(): StockAdjustment {
        return copy(
            syncStatus = SyncStatus.PENDING,
            oldQuantityBase = oldStock.toLong(),
            newQuantityBase = newStock.toLong(),
            differenceBase = difference.toLong()
        )
    }
}
