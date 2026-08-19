package com.aistudio.shreeshyamstore.pqwzkb.data

import androidx.room.withTransaction
import com.aistudio.shreeshyamstore.pqwzkb.commerce.InventoryValidation
import com.aistudio.shreeshyamstore.pqwzkb.commerce.LedgerActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.LedgerAuditPolicy
import com.aistudio.shreeshyamstore.pqwzkb.commerce.UdhaarTransactionType
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncIdentity
import kotlinx.coroutines.flow.Flow

class ShopRepository(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao,
    private val udhaarDao: UdhaarDao,
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val userDao: UserDao,
    private val database: AppDatabase? = null,
    private val shopProfileDao: ShopProfileDao? = null,
    private val settingsDataStore: SettingsDataStore? = null
) {
    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    
    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    suspend fun getCategoryByName(name: String): Category? = categoryDao.getCategoryByName(name)

    suspend fun insertCategory(category: Category): Long = inCatalogTransaction {
        val normalized = normalizeCategory(category)
        require(categoryDao.getCategoryByNameExcludingId(normalized.name, 0L) == null) {
            "Category name already exists"
        }
        categoryDao.insert(normalized.stamped(mutationDeviceId()))
    }

    suspend fun updateCategory(category: Category) = inCatalogTransaction {
        val normalized = normalizeCategory(category)
        require(categoryDao.getCategoryByNameExcludingId(normalized.name, category.id) == null) {
            "Category name already exists"
        }
        categoryDao.update(normalized.stamped(mutationDeviceId()))
    }

    suspend fun deleteCategory(category: Category) =
        categoryDao.update(category.stamped(mutationDeviceId(), isDeleted = true))

    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)
    fun getProductByIdFlow(id: Long): Flow<Product?> = productDao.getProductByIdFlow(id)
    fun getProductsByCategory(categoryId: Long): Flow<List<Product>> = productDao.getProductsByCategory(categoryId)

    suspend fun insertProduct(product: Product): Long = inCatalogTransaction {
        val normalized = normalizeProduct(product)
        requireNoDuplicateBarcode(normalized.barcodeKey, 0L)
        productDao.insert(normalized.stamped(mutationDeviceId()))
    }

    suspend fun insertProductWithOpeningStock(
        product: Product,
        openingStock: Double,
        createdAt: Long = System.currentTimeMillis()
    ): Long = inCatalogTransaction {
        val normalizedStock = InventoryValidation.validateQuantity(openingStock, "Opening stock")
        val normalized = normalizeProduct(product.copy(currentStock = normalizedStock))
        requireNoDuplicateBarcode(normalized.barcodeKey, 0L)
        val productId = productDao.insert(normalized.stamped(mutationDeviceId()))
        if (normalized.trackStock && normalizedStock > 0.0) {
            stockAdjustmentDao.insertAdjustment(
                StockAdjustment(
                    productId = productId,
                    oldStock = 0.0,
                    newStock = normalizedStock,
                    difference = normalizedStock,
                    reason = "Opening stock entry",
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    isSynced = false
                ).stamped(mutationDeviceId())
            )
        }
        productId
    }

    suspend fun updateProduct(product: Product) = inCatalogTransaction {
        val normalized = normalizeProduct(product)
        requireNoDuplicateBarcode(normalized.barcodeKey, product.id)
        productDao.update(normalized.stamped(mutationDeviceId()))
    }

    suspend fun updateProductWithStockAdjustment(
        product: Product,
        oldStock: Double,
        newStock: Double,
        reason: String,
        createdAt: Long = System.currentTimeMillis()
    ) = inCatalogTransaction {
        val normalizedOldStock = InventoryValidation.validateQuantity(oldStock, "Old stock")
        val normalizedNewStock = InventoryValidation.validateQuantity(newStock, "New stock")
        val normalized = normalizeProduct(product.copy(currentStock = normalizedNewStock))
        requireNoDuplicateBarcode(normalized.barcodeKey, product.id)
        productDao.update(normalized.stamped(mutationDeviceId()))
        if (normalized.trackStock && normalizedOldStock != normalizedNewStock) {
            stockAdjustmentDao.insertAdjustment(
                StockAdjustment(
                    productId = product.id,
                    oldStock = normalizedOldStock,
                    newStock = normalizedNewStock,
                    difference = normalizedNewStock - normalizedOldStock,
                    reason = InventoryValidation.validateReason(reason),
                    createdAt = createdAt,
                    updatedAt = createdAt,
                    isSynced = false
                ).stamped(mutationDeviceId())
            )
        }
    }

    suspend fun isBarcodeAvailable(barcode: String, excludeId: Long = 0L): Boolean {
        val barcodeKey = InventoryValidation.normalizeBarcode(barcode) ?: return true
        return productDao.getActiveProductByBarcodeKey(barcodeKey, excludeId) == null &&
            productDao.getActiveProductByLegacyBarcode(barcodeKey, excludeId) == null
    }

    private suspend fun requireNoDuplicateBarcode(barcodeKey: String?, excludeId: Long) {
        require(barcodeKey == null || isBarcodeAvailable(barcodeKey, excludeId)) {
            "Barcode already belongs to another active product"
        }
    }

    private fun normalizeCategory(category: Category): Category = category.copy(
        name = InventoryValidation.validateCategoryName(category.name)
    )

    private fun normalizeProduct(product: Product): Product {
        val normalizedBarcode = InventoryValidation.normalizeBarcode(product.barcode)
        return product.copy(
            name = InventoryValidation.validateProductName(product.name),
            mrp = InventoryValidation.validateProductMoney(product.mrp, "MRP"),
            sellingPrice = InventoryValidation.validateOptionalMoney(product.sellingPrice, "Selling price"),
            purchasePrice = InventoryValidation.validateOptionalMoney(product.purchasePrice, "Purchase price"),
            currentStock = InventoryValidation.validateQuantity(product.currentStock, "Current stock"),
            lowStockAlertQty = InventoryValidation.validateQuantity(product.lowStockAlertQty, "Low-stock alert quantity"),
            unit = InventoryValidation.validateUnit(product.unit),
            barcode = product.barcode.trim(),
            barcodeKey = normalizedBarcode
        )
    }

    private suspend fun <T> inCatalogTransaction(block: suspend () -> T): T =
        if (database != null) database.withTransaction { block() } else block()

    // Sales
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    
    suspend fun getSaleById(id: Long): Sale? = saleDao.getSaleById(id)
    fun getSaleItemsForSale(saleId: Long): Flow<List<SaleItem>> = saleDao.getSaleItemsForSale(saleId)
    suspend fun getSaleItemsForSaleList(saleId: Long): List<SaleItem> = saleDao.getSaleItemsForSaleList(saleId)
    fun getSalesForDateRange(start: Long, end: Long): Flow<List<Sale>> = saleDao.getSalesForDateRange(start, end)

    /**
     * Executes atomic bill checkout transaction in Room:
     * 1. Inserts Bill/Sale record
     * 2. Inserts all BillItems/SaleItems line entries
     * 3. Deducts sold quantity from Product.currentStock for tracked products
     * 4. Logs StockAdjustment audit history record
     * 5. If payment method is UDHAAR, logs UdhaarTransaction credit record & updates customer
     * 
     * Uses Room's @Transaction on the DAO layer to guarantee zero partial writes.
     */
    suspend fun completeBillCheckout(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerId: Long? = null,
        ledgerActor: LedgerActor? = null
    ): Long {
        val deviceId = mutationDeviceId()
        val stampedSale = sale.stamped(deviceId)
        val stampedItems = items.map { it.stamped(deviceId) }
        return saleDao.completeBillCheckout(stampedSale, stampedItems, selectedCustomerId, ledgerActor)
    }

    suspend fun insertSaleWithItems(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerId: Long? = null,
        ledgerActor: LedgerActor? = null
    ): Long {
        return completeBillCheckout(sale, items, selectedCustomerId, ledgerActor)
    }

    suspend fun insertSaleWithNewCustomer(
        sale: Sale,
        items: List<SaleItem>,
        newCustomer: Customer,
        ledgerActor: LedgerActor? = null
    ): Long {
        val deviceId = mutationDeviceId()
        val stampedSale = sale.stamped(deviceId)
        val stampedItems = items.map { it.stamped(deviceId) }
        return saleDao.completeBillCheckoutWithNewCustomer(
            stampedSale,
            stampedItems,
            newCustomer.stamped(deviceId),
            ledgerActor
        )
    }

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    
    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)
    suspend fun getCustomerByName(name: String): Customer? = customerDao.getCustomerByName(name)
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer.stamped(mutationDeviceId()))
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer.stamped(mutationDeviceId()))
    suspend fun deleteCustomer(customer: Customer) = customerDao.updateCustomer(customer.stamped(mutationDeviceId(), isDeleted = true))

    // Udhaar
    val allUdhaarTransactions: Flow<List<UdhaarTransaction>> = udhaarDao.getAllTransactions()
    
    fun getTransactionsForCustomer(customerId: Long): Flow<List<UdhaarTransaction>> = 
        udhaarDao.getTransactionsForCustomer(customerId)

    suspend fun getTransactionsForCustomerList(customerId: Long): List<UdhaarTransaction> = 
        udhaarDao.getTransactionsForCustomerList(customerId)

    fun getCustomerBalanceFlow(customerId: Long): Flow<Long> =
        udhaarDao.getCustomerBalanceFlow(customerId)

    suspend fun getCustomerBalance(customerId: Long): Long =
        udhaarDao.getCustomerBalance(customerId)

    fun getTotalUdhaarFlow(): Flow<Long> =
        udhaarDao.getTotalUdhaarFlow()

    suspend fun getTotalUdhaar(): Long =
        udhaarDao.getTotalUdhaar()

    suspend fun recordUdhaarPayment(
        customerId: Long,
        amountMinorUnits: Long,
        note: String?,
        actor: LedgerActor
    ): Long {
        val normalizedActor = LedgerAuditPolicy.requireCanRecord(actor)
        return inLedgerTransaction {
            require(amountMinorUnits > 0L) {
                "Payment amount must be positive"
            }
            require(customerDao.getCustomerById(customerId)?.isDeleted == false) {
                "Payment requires an active customer"
            }
            val now = System.currentTimeMillis()
            val transactionId = udhaarDao.insertTransaction(
                UdhaarTransaction(
                    globalId = SyncIdentity.newGlobalId(),
                    customerId = customerId,
                    type = UdhaarTransactionType.PAYMENT.name,
                    amount = amountMinorUnits,
                    balanceEffect = -amountMinorUnits,
                    note = note?.trim()?.ifEmpty { "Payment received" } ?: "Payment received",
                    actorUid = normalizedActor.actorUid,
                    actorName = normalizedActor.actorName,
                    actorRole = normalizedActor.actorRole,
                    actorDeviceId = normalizedActor.actorDeviceId,
                    mutationVersion = now,
                    mutationDeviceId = normalizedActor.actorDeviceId,
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            customerDao.touchCustomer(customerId, now, normalizedActor.actorDeviceId)
            transactionId
        }
    }

    suspend fun reverseUdhaarTransaction(
        customerId: Long,
        eventId: String,
        reason: String,
        actor: LedgerActor
    ): Long {
        val normalizedActor = LedgerAuditPolicy.requireCanCorrect(actor)
        val normalizedReason = LedgerAuditPolicy.requireReason(reason)
        return inLedgerTransaction {
            val target = udhaarDao.getActiveEventById(eventId)
            require(target != null && target.customerId == customerId) {
                "Ledger event was not found for this customer"
            }
            require(target.type == UdhaarTransactionType.CREDIT.name || target.type == UdhaarTransactionType.PAYMENT.name) {
                "Only original ledger events can be reversed"
            }
            require(udhaarDao.countActiveCorrectionsFor(target.eventId) == 0) {
                "Ledger event has already been corrected"
            }
            val now = System.currentTimeMillis()
            val reversalId = udhaarDao.insertTransaction(
                UdhaarTransaction(
                    globalId = SyncIdentity.newGlobalId(),
                    customerId = customerId,
                    saleId = target.saleId,
                    type = UdhaarTransactionType.REVERSAL.name,
                    amount = target.amount,
                    balanceEffect = -target.balanceEffect,
                    note = "Reversal of ${target.eventId}",
                    correctsEventId = target.eventId,
                    correctionReason = normalizedReason,
                    actorUid = normalizedActor.actorUid,
                    actorName = normalizedActor.actorName,
                    actorRole = normalizedActor.actorRole,
                    actorDeviceId = normalizedActor.actorDeviceId,
                    mutationVersion = now,
                    mutationDeviceId = normalizedActor.actorDeviceId,
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            customerDao.touchCustomer(customerId, now, normalizedActor.actorDeviceId)
            reversalId
        }
    }

    suspend fun correctUdhaarTransaction(
        customerId: Long,
        eventId: String,
        correctedAmountMinorUnits: Long,
        reason: String,
        actor: LedgerActor
    ): Long {
        val normalizedActor = LedgerAuditPolicy.requireCanCorrect(actor)
        val normalizedReason = LedgerAuditPolicy.requireReason(reason)
        require(correctedAmountMinorUnits > 0L) { "Corrected amount must be positive" }
        return inLedgerTransaction {
            val target = udhaarDao.getActiveEventById(eventId)
            require(target != null && target.customerId == customerId) {
                "Ledger event was not found for this customer"
            }
            require(target.type == UdhaarTransactionType.CREDIT.name || target.type == UdhaarTransactionType.PAYMENT.name) {
                "Only original ledger events can be corrected"
            }
            require(udhaarDao.countActiveCorrectionsFor(target.eventId) == 0) {
                "Ledger event has already been corrected"
            }
            val replacementEffect = if (target.balanceEffect >= 0L) {
                correctedAmountMinorUnits
            } else {
                -correctedAmountMinorUnits
            }
            val now = System.currentTimeMillis()
            val correctionId = udhaarDao.insertTransaction(
                UdhaarTransaction(
                    globalId = SyncIdentity.newGlobalId(),
                    customerId = customerId,
                    saleId = target.saleId,
                    type = UdhaarTransactionType.CORRECTION.name,
                    amount = correctedAmountMinorUnits,
                    balanceEffect = replacementEffect - target.balanceEffect,
                    note = "Correction of ${target.eventId}",
                    correctsEventId = target.eventId,
                    correctionReason = normalizedReason,
                    actorUid = normalizedActor.actorUid,
                    actorName = normalizedActor.actorName,
                    actorRole = normalizedActor.actorRole,
                    actorDeviceId = normalizedActor.actorDeviceId,
                    mutationVersion = now,
                    mutationDeviceId = normalizedActor.actorDeviceId,
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            customerDao.touchCustomer(customerId, now, normalizedActor.actorDeviceId)
            correctionId
        }
    }

    private suspend fun <T> inLedgerTransaction(operation: suspend () -> T): T =
        database?.withTransaction { operation() } ?: operation()

    private suspend fun mutationDeviceId(): String =
        settingsDataStore?.getOrCreateAuditDeviceId() ?: SyncIdentity.LEGACY_DEVICE_ID

    private fun Category.stamped(deviceId: String, isDeleted: Boolean = this.isDeleted): Category {
        val now = maxOf(System.currentTimeMillis(), mutationVersion + 1L)
        return copy(
            globalId = globalId.ifBlank { SyncIdentity.newGlobalId() },
            updatedAt = now,
            mutationVersion = now,
            mutationDeviceId = deviceId,
            isDeleted = isDeleted,
            isSynced = false
        )
    }

    private fun Product.stamped(deviceId: String, isDeleted: Boolean = this.isDeleted): Product {
        val now = maxOf(System.currentTimeMillis(), mutationVersion + 1L)
        return copy(
            globalId = globalId.ifBlank { SyncIdentity.newGlobalId() },
            updatedAt = now,
            mutationVersion = now,
            mutationDeviceId = deviceId,
            isDeleted = isDeleted,
            isSynced = false
        )
    }

    private fun Sale.stamped(deviceId: String): Sale {
        val now = maxOf(System.currentTimeMillis(), mutationVersion + 1L)
        return copy(
            globalId = globalId.ifBlank { SyncIdentity.newGlobalId() },
            updatedAt = now,
            mutationVersion = now,
            mutationDeviceId = deviceId,
            isSynced = false
        )
    }

    private fun SaleItem.stamped(deviceId: String): SaleItem {
        val now = maxOf(System.currentTimeMillis(), mutationVersion + 1L)
        return copy(
            globalId = globalId.ifBlank { SyncIdentity.newGlobalId() },
            updatedAt = now,
            mutationVersion = now,
            mutationDeviceId = deviceId,
            isSynced = false
        )
    }

    private fun Customer.stamped(deviceId: String, isDeleted: Boolean = this.isDeleted): Customer {
        val now = maxOf(System.currentTimeMillis(), mutationVersion + 1L)
        return copy(
            globalId = globalId.ifBlank { SyncIdentity.newGlobalId() },
            updatedAt = now,
            mutationVersion = now,
            mutationDeviceId = deviceId,
            isDeleted = isDeleted,
            isSynced = false
        )
    }

    private fun StockAdjustment.stamped(deviceId: String): StockAdjustment {
        val now = maxOf(System.currentTimeMillis(), mutationVersion + 1L)
        return copy(
            globalId = globalId.ifBlank { SyncIdentity.newGlobalId() },
            updatedAt = now,
            mutationVersion = now,
            mutationDeviceId = deviceId,
            isSynced = false
        )
    }

    // Stock Adjustments
    val allStockAdjustments: Flow<List<StockAdjustment>> = stockAdjustmentDao.getAllAdjustments()
    
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>> = 
        stockAdjustmentDao.getAdjustmentsForProduct(productId)

    suspend fun insertStockAdjustment(adjustment: StockAdjustment): Long {
        val normalized = adjustment.copy(
            oldStock = InventoryValidation.validateQuantity(adjustment.oldStock, "Old stock"),
            newStock = InventoryValidation.validateQuantity(adjustment.newStock, "New stock"),
            difference = requireFiniteStockDelta(adjustment.difference),
            reason = InventoryValidation.validateReason(adjustment.reason)
        )
        return stockAdjustmentDao.insertAdjustment(normalized.stamped(mutationDeviceId()))
    }

    /** Corrects a product stock level and logs the audit record atomically. */
    suspend fun adjustProductStock(productId: Long, actualStockCounted: Double, reason: String) {
        val validatedStock = InventoryValidation.validateQuantity(actualStockCounted, "Stock")
        val validatedReason = InventoryValidation.validateReason(reason)
        val operation: suspend () -> Unit = {
            val product = productDao.getProductById(productId)
            require(product != null && product.isActive && !product.isDeleted) {
                "Stock adjustment requires an active product"
            }
            val oldStock = product.currentStock
            val now = System.currentTimeMillis()
            val deviceId = mutationDeviceId()
            productDao.update(
                product.copy(
                    currentStock = validatedStock,
                    updatedAt = now,
                    mutationVersion = now,
                    mutationDeviceId = deviceId,
                    isSynced = false
                )
            )
            stockAdjustmentDao.insertAdjustment(
                StockAdjustment(
                    productId = productId,
                    oldStock = oldStock,
                    newStock = validatedStock,
                    difference = validatedStock - oldStock,
                    reason = validatedReason,
                    mutationVersion = now,
                    mutationDeviceId = deviceId,
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        if (database != null) database.withTransaction { operation() } else operation()
    }

    private fun requireFiniteStockDelta(value: Double): Double {
        require(value.isFinite()) { "Stock difference must be finite" }
        return value
    }


    suspend fun getShopProfile(uid: String): ShopProfile? =
        shopProfileDao?.getByUid(uid) ?: database?.shopProfileDao()?.getByUid(uid)

    suspend fun saveShopProfile(profile: ShopProfile) {
        shopProfileDao?.upsert(profile) ?: database?.shopProfileDao()?.upsert(profile)
    }

    // --- User Authentication / Session Management Functions ---
    suspend fun getUserByUsernameOrEmail(username: String, email: String): User? = userDao.getUserByUsernameOrEmail(username, email)
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)
    suspend fun getAllUsers(): List<User> = userDao.getAllUsersList()
    suspend fun getAllStockAdjustmentsList(): List<StockAdjustment> = stockAdjustmentDao.getAllAdjustmentsList()

    // --- Sync Engine Queries & Operations ---
    suspend fun getUnsyncedCategories(): List<Category> = categoryDao.getUnsyncedCategories()
    suspend fun markCategoriesSynced(ids: List<Long>) = categoryDao.markCategoriesSynced(ids)

    suspend fun getUnsyncedProducts(): List<Product> = productDao.getUnsyncedProducts()
    suspend fun markProductsSynced(ids: List<Long>) = productDao.markProductsSynced(ids)

    suspend fun getUnsyncedSales(): List<Sale> = saleDao.getUnsyncedSales()
    suspend fun markSalesSynced(ids: List<Long>) = saleDao.markSalesSynced(ids)

    suspend fun getUnsyncedSaleItems(): List<SaleItem> = saleDao.getUnsyncedSaleItems()
    suspend fun markSaleItemsSynced(ids: List<Long>) = saleDao.markSaleItemsSynced(ids)

    suspend fun getUnsyncedCustomers(): List<Customer> = customerDao.getUnsyncedCustomers()
    suspend fun markCustomersSynced(ids: List<Long>) = customerDao.markCustomersSynced(ids)

    suspend fun getUnsyncedUdhaarTransactions(): List<UdhaarTransaction> = udhaarDao.getUnsyncedTransactions()
    suspend fun markUdhaarTransactionsSynced(ids: List<Long>) = udhaarDao.markTransactionsSynced(ids)

    suspend fun getUnsyncedStockAdjustments(): List<StockAdjustment> = stockAdjustmentDao.getUnsyncedAdjustments()
    suspend fun markStockAdjustmentsSynced(ids: List<Long>) = stockAdjustmentDao.markAdjustmentsSynced(ids)

    suspend fun getUnsyncedUsers(): List<User> = userDao.getUnsyncedUsers()
    suspend fun markUsersSynced(ids: List<Long>) = userDao.markUsersSynced(ids)

    /**
     * Atomically replaces cloud-owned business tables during a restore.
     * Device-local identity/session records and the shop profile are preserved.
     */
    suspend fun replaceCloudRestorableTables(
        categoriesList: List<Category>,
        productsList: List<Product>,
        salesList: List<Sale>,
        saleItemsList: List<SaleItem>,
        customersList: List<Customer>,
        udhaarTxsList: List<UdhaarTransaction>,
        adjustmentsList: List<StockAdjustment>
    ) {
        val normalizedCategories = categoriesList.map { it.normalizeForRestore("categories") }
        val normalizedProducts = productsList.map { it.normalizeForRestore("products") }
        val normalizedSales = salesList.map { it.normalizeForRestore("sales") }
        val normalizedSaleItems = saleItemsList.map { it.normalizeForRestore("sale_items") }
        val normalizedCustomers = customersList.map { it.normalizeForRestore("customers") }
        val normalizedUdhaar = udhaarTxsList.map { it.normalizeForRestore("udhaar_transactions") }
        val normalizedAdjustments = adjustmentsList.map { it.normalizeForRestore("stock_adjustments") }
        val operation: suspend () -> Unit = {
            categoryDao.clearAllCategories()
            productDao.clearAllProducts()
            saleDao.clearAllSales()
            saleDao.clearAllSaleItems()
            customerDao.clearAllCustomers()
            udhaarDao.clearAllTransactions()
            stockAdjustmentDao.clearAllAdjustments()
            syncOutboxDao().clearAll()

            if (normalizedCategories.isNotEmpty()) categoryDao.insertAll(normalizedCategories)
            if (normalizedProducts.isNotEmpty()) productDao.insertAll(normalizedProducts)
            if (normalizedSales.isNotEmpty()) saleDao.insertAllSales(normalizedSales)
            if (normalizedSaleItems.isNotEmpty()) saleDao.insertAllSaleItems(normalizedSaleItems)
            if (normalizedCustomers.isNotEmpty()) customerDao.insertAll(normalizedCustomers)
            if (normalizedUdhaar.isNotEmpty()) udhaarDao.insertAll(normalizedUdhaar)
            if (normalizedAdjustments.isNotEmpty()) stockAdjustmentDao.insertAll(normalizedAdjustments)
        }
        if (database != null) database.withTransaction { operation() } else operation()
    }

    private suspend fun syncOutboxDao(): SyncOutboxDao =
        database?.syncOutboxDao() ?: error("Sync outbox requires a database")

    private fun Category.normalizeForRestore(tableName: String): Category = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    private fun Product.normalizeForRestore(tableName: String): Product = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    private fun Sale.normalizeForRestore(tableName: String): Sale = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    private fun SaleItem.normalizeForRestore(tableName: String): SaleItem = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    private fun Customer.normalizeForRestore(tableName: String): Customer = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    private fun UdhaarTransaction.normalizeForRestore(tableName: String): UdhaarTransaction = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    private fun StockAdjustment.normalizeForRestore(tableName: String): StockAdjustment = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    suspend fun getAllSaleItems(): List<SaleItem> = saleDao.getAllSaleItemsList()
}

