package com.sevenzenlabs.zenmart.data

import androidx.room.withTransaction
import com.sevenzenlabs.zenmart.commerce.CommandMetadata
import com.sevenzenlabs.zenmart.commerce.CommerceValidation
import com.sevenzenlabs.zenmart.commerce.InventoryValidation
import com.sevenzenlabs.zenmart.commerce.LedgerActor
import com.sevenzenlabs.zenmart.commerce.LedgerAuditPolicy
import com.sevenzenlabs.zenmart.commerce.PaymentMode
import com.sevenzenlabs.zenmart.commerce.PaymentState
import com.sevenzenlabs.zenmart.commerce.PlatformActor
import com.sevenzenlabs.zenmart.commerce.TenantAuthorizationPolicy
import com.sevenzenlabs.zenmart.commerce.TenantCapability
import com.sevenzenlabs.zenmart.commerce.TenantScope
import com.sevenzenlabs.zenmart.commerce.UdhaarTransactionType
import com.sevenzenlabs.zenmart.utils.BusinessRelationshipPolicy
import com.sevenzenlabs.zenmart.utils.SyncIdentity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

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
    private val settingsDataStore: SettingsDataStore? = null,
    private val authorizationContextProvider: (suspend () -> Pair<TenantScope, PlatformActor>)? = null,
    private val returnDao: ReturnDao? = null
) {
    private val activeReturnDao: ReturnDao
        get() = returnDao ?: database?.returnDao() ?: error("ReturnDao requires an initialized database or returnDao instance")

    fun observeSyncOutboxSummary(): Flow<SyncOutboxSummary> =
        database?.syncOutboxDao()?.observeSummary() ?: flowOf(SyncOutboxSummary())

    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    
    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    suspend fun getCategoryByName(name: String): Category? = categoryDao.getCategoryByName(name)

    suspend fun insertCategory(category: Category, command: CommandMetadata): Long {
        authorize(command, TenantCapability.CATALOG_WRITE)
        return inCatalogTransaction {
            val normalized = normalizeCategory(category)
            require(categoryDao.getCategoryByNameExcludingId(normalized.name, 0L) == null) {
                "Category name already exists"
            }
            categoryDao.insert(normalized.stamped(mutationDeviceId()))
        }
    }

    suspend fun updateCategory(category: Category, command: CommandMetadata) {
        authorize(command, TenantCapability.CATALOG_WRITE)
        inCatalogTransaction {
            val normalized = normalizeCategory(category)
            require(categoryDao.getCategoryByNameExcludingId(normalized.name, category.id) == null) {
                "Category name already exists"
            }
            categoryDao.update(normalized.stamped(mutationDeviceId()))
        }
    }

    suspend fun deleteCategory(category: Category, command: CommandMetadata) {
        authorize(command, TenantCapability.CATALOG_WRITE)
        inCatalogTransaction {
            require(productDao.countActiveByCategoryId(category.id) == 0) {
                "Category cannot be deleted while active products use it"
            }
            categoryDao.update(category.stamped(mutationDeviceId(), isDeleted = true))
        }
    }

    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)
    fun getProductByIdFlow(id: Long): Flow<Product?> = productDao.getProductByIdFlow(id)
    fun getProductsByCategory(categoryId: Long): Flow<List<Product>> = productDao.getProductsByCategory(categoryId)

    suspend fun insertProduct(product: Product, command: CommandMetadata): Long {
        authorize(command, TenantCapability.CATALOG_WRITE)
        return inCatalogTransaction {
            val normalized = normalizeProduct(product)
            requireActiveCategory(normalized.categoryId)
            requireNoDuplicateBarcode(normalized.barcodeKey, 0L)
            productDao.insert(normalized.stamped(mutationDeviceId()))
        }
    }

    suspend fun insertProductWithOpeningStock(
        product: Product,
        openingStock: Double,
        createdAt: Long = System.currentTimeMillis(),
        command: CommandMetadata
    ): Long {
        authorize(command, TenantCapability.CATALOG_WRITE)
        return inCatalogTransaction {
        val normalized = normalizeProduct(product.copy(currentStock = openingStock))
        val normalizedStock = if (normalized.trackStock) {
            InventoryValidation.validateQuantityForUnit(openingStock, "Opening stock", normalized.unit)
        } else {
            InventoryValidation.validateQuantity(openingStock, "Opening stock")
        }
        requireActiveCategory(normalized.categoryId)
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
    }

    suspend fun updateProduct(product: Product, command: CommandMetadata) {
        authorize(command, TenantCapability.CATALOG_WRITE)
        inCatalogTransaction {
            val normalized = normalizeProduct(product)
            requireActiveCategory(normalized.categoryId)
            requireNoDuplicateBarcode(normalized.barcodeKey, product.id)
            productDao.update(normalized.stamped(mutationDeviceId()))
        }
    }

    suspend fun updateProductWithStockAdjustment(
        product: Product,
        oldStock: Double,
        newStock: Double,
        reason: String,
        createdAt: Long = System.currentTimeMillis(),
        command: CommandMetadata
    ) {
        authorize(command, TenantCapability.CATALOG_WRITE)
        inCatalogTransaction {
            val normalized = normalizeProduct(product.copy(currentStock = newStock))
            val normalizedOldStock = if (normalized.trackStock) {
                InventoryValidation.validateQuantityForUnit(oldStock, "Old stock", normalized.unit)
            } else {
                InventoryValidation.validateQuantity(oldStock, "Old stock")
            }
            val normalizedNewStock = normalized.currentStock
            requireActiveCategory(normalized.categoryId)
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
    }

    suspend fun isBarcodeAvailable(barcode: String, excludeId: Long = 0L): Boolean {
        val barcodeKey = InventoryValidation.normalizeBarcode(barcode) ?: return true
        return productDao.getActiveProductByBarcodeKey(barcodeKey, excludeId) == null &&
            productDao.getActiveProductByLegacyBarcode(barcodeKey, excludeId) == null
    }

    private suspend fun requireActiveCategory(categoryId: Long) {
        require(categoryId > 0L && categoryDao.getCategoryById(categoryId)?.isDeleted == false) {
            "Product requires an active category"
        }
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
        val normalizedUnit = InventoryValidation.validateRequiredUnit(product.unit)
        val normalizedBarcode = InventoryValidation.validateOptionalBarcode(product.barcode)
        return product.copy(
            name = InventoryValidation.validateProductName(product.name),
            mrp = InventoryValidation.validateRequiredProductMoney(product.mrp, "MRP"),
            sellingPrice = InventoryValidation.validateOptionalMoney(product.sellingPrice, "Selling price"),
            purchasePrice = InventoryValidation.validateOptionalMoney(product.purchasePrice, "Purchase price"),
            currentStock = if (product.trackStock) {
                InventoryValidation.validateQuantityForUnit(product.currentStock, "Current stock", normalizedUnit)
            } else {
                InventoryValidation.validateQuantity(product.currentStock, "Current stock")
            },
            lowStockAlertQty = if (product.trackStock) {
                InventoryValidation.validateQuantityForUnit(product.lowStockAlertQty, "Low-stock alert quantity", normalizedUnit)
            } else {
                InventoryValidation.validateQuantity(product.lowStockAlertQty, "Low-stock alert quantity")
            },
            unit = normalizedUnit,
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

    suspend fun reconcilePaymentState(
        saleId: Long,
        targetState: PaymentState,
        receivedAmount: Long,
        command: CommandMetadata
    ): Sale {
        val authorizedCommand = authorize(command, TenantCapability.PAYMENT_RECONCILIATION)
        val normalizedActor = LedgerAuditPolicy.requireCanRecord(authorizedCommand.toLedgerActor())
        val sale = saleDao.getSaleById(saleId)
        require(sale != null && !sale.isDeleted) { "Sale was not found" }
        val normalizedReceived = CommerceValidation.validatePaymentStateTransition(
            paymentModeRaw = sale.paymentMode,
            currentStateRaw = sale.paymentState,
            targetState = targetState,
            totalAmount = sale.totalAmount,
            receivedAmount = receivedAmount
        )
        val now = System.currentTimeMillis()
        val updated = saleDao.updatePaymentStateIfActive(
            saleId = saleId,
            paymentState = targetState.wireValue,
            receivedAmount = if (targetState == PaymentState.FAILED) null else normalizedReceived,
            updatedAt = now,
            mutationDeviceId = authorizedCommand.actor.deviceId
        )
        require(updated == 1) { "Payment state update was not applied" }
        return saleDao.getSaleById(saleId) ?: error("Sale disappeared after payment update")
    }
    suspend fun getSaleItemsForSaleList(saleId: Long): List<SaleItem> = saleDao.getSaleItemsForSaleList(saleId)
    fun getSalesForDateRange(start: Long, end: Long): Flow<List<Sale>> = saleDao.getSalesForDateRange(start, end)

    // Returns & Refunds
    val allReturns: Flow<List<Return>>
        get() = activeReturnDao.getAllReturns()

    fun getReturnsForSale(saleId: Long): Flow<List<Return>> =
        activeReturnDao.getReturnsForSale(saleId)

    suspend fun getReturnsForSaleList(saleId: Long): List<Return> =
        activeReturnDao.getReturnsForSaleList(saleId)

    fun getReturnItemsForReturn(returnId: Long): Flow<List<ReturnItem>> =
        activeReturnDao.getReturnItemsForReturn(returnId)

    suspend fun getReturnItemsForReturnList(returnId: Long): List<ReturnItem> =
        activeReturnDao.getReturnItemsForReturnList(returnId)

    fun getReturnsForDateRange(start: Long, end: Long): Flow<List<Return>> =
        activeReturnDao.getReturnsForDateRange(start, end)

    fun getTotalRefundsForDateRange(start: Long, end: Long): Flow<Long> =
        activeReturnDao.getTotalRefundsForDateRange(start, end)

    suspend fun getRemainingReturnableQuantities(saleId: Long): Map<Long, Double> {
        val items = saleDao.getSaleItemsForSaleList(saleId)
        val rDao = activeReturnDao
        return items.associate { item ->
            val returned = rDao.getReturnedQuantityForSaleItem(item.id)
            item.id to maxOf(0.0, item.quantity - returned)
        }
    }

    /**
     * Executes atomic return and refund transaction in local Room:
     * 1. Validates operator authority (RETURN_PROCESSING capability).
     * 2. Validates sale existence and that requested quantities do not exceed remaining returnable quantities.
     * 3. Calculates line refund amounts and total refund in integer paise.
     * 4. Inserts Return and ReturnItem records stamped with audit metadata.
     * 5. Replenishes Product.currentStock for tracked items and records StockAdjustment audit entries.
     * 6. Reverses customer debt via UdhaarTransaction (REVERSAL) if the sale was on Udhaar.
     * 7. Transitions Sale.paymentState to REFUNDED or PARTIALLY_REFUNDED and updates receivedAmount.
     */
    suspend fun processReturn(
        saleId: Long,
        itemsToReturn: List<ItemReturnRequest>,
        refundMode: String,
        reason: String,
        note: String? = null,
        command: CommandMetadata
    ): ReturnResult {
        val authorizedCommand = authorize(command, TenantCapability.RETURN_PROCESSING)
        val deviceId = authorizedCommand.actor.deviceId
        val ledgerActor = authorizedCommand.toLedgerActor()

        require(itemsToReturn.isNotEmpty()) { "Return must contain at least one item" }
        require(itemsToReturn.map { it.saleItemId }.toSet().size == itemsToReturn.size) {
            "Duplicate line items in return request"
        }
        require(reason.isNotBlank()) { "Return reason is required" }
        require(refundMode.isNotBlank()) { "Refund mode is required" }

        val operation: suspend () -> ReturnResult = {
            val sale = saleDao.getSaleById(saleId)
                ?: throw IllegalArgumentException("Sale not found")
            require(!sale.isDeleted) { "Cannot process return on a deleted sale" }
            require(sale.paymentState != PaymentState.REFUNDED.wireValue) {
                "Sale is already fully refunded"
            }

            val existingSaleItems = saleDao.getSaleItemsForSaleList(saleId)
            require(existingSaleItems.isNotEmpty()) { "Sale has no line items" }

            val now = System.currentTimeMillis()
            val rDao = activeReturnDao
            var totalRefundPaise = 0L
            val returnItemsToInsert = mutableListOf<ReturnItem>()
            val stockRestorations = mutableListOf<Pair<Product, Double>>()

            for (req in itemsToReturn) {
                require(req.quantityReturned.isFinite() && req.quantityReturned > 0.0) {
                    "Quantity returned must be positive"
                }
                val saleItem = existingSaleItems.find { it.id == req.saleItemId }
                    ?: throw IllegalArgumentException("Sale item ${req.saleItemId} does not belong to sale $saleId")

                val cumulativeReturned = rDao.getReturnedQuantityForSaleItem(saleItem.id)
                val remainingReturnable = saleItem.quantity - cumulativeReturned
                require(req.quantityReturned <= remainingReturnable + 1e-4) {
                    "Cannot return ${req.quantityReturned} of ${saleItem.productNameSnapshot}: only $remainingReturnable remaining"
                }

                val lineRefund = CommerceValidation.calculateLineTotal(saleItem.unitPrice, req.quantityReturned)
                totalRefundPaise += lineRefund

                val item = ReturnItem(
                    returnId = 0L,
                    saleItemId = saleItem.id,
                    productId = saleItem.productId,
                    productNameSnapshot = saleItem.productNameSnapshot,
                    quantityReturned = req.quantityReturned,
                    unit = saleItem.unit,
                    unitPrice = saleItem.unitPrice,
                    lineRefundTotal = lineRefund,
                    updatedAt = now
                ).stamped(deviceId)
                returnItemsToInsert.add(item)

                val product = productDao.getProductById(saleItem.productId)
                if (product != null && product.trackStock) {
                    stockRestorations.add(product to req.quantityReturned)
                }
            }

            require(totalRefundPaise >= 0L) { "Total refund amount cannot be negative" }

            val returnSuffix = java.util.UUID.randomUUID().toString().take(6).uppercase()
            val returnNumber = "RET-${sale.billNumber}-$returnSuffix"
            val returnRecord = Return(
                returnNumber = returnNumber,
                saleId = sale.id,
                originalBillNumber = sale.billNumber,
                customerId = sale.customerId,
                totalRefundAmount = totalRefundPaise,
                refundMode = refundMode.trim(),
                refundState = "RECORDED",
                reason = reason.trim(),
                note = note?.trim().orEmpty(),
                createdAt = now,
                updatedAt = now
            ).stamped(deviceId)

            val returnId = rDao.insertReturn(returnRecord)
            val finalizedItems = returnItemsToInsert.map { it.copy(returnId = returnId) }
            rDao.insertReturnItems(finalizedItems)

            // Replenish stock for tracked products and log StockAdjustment
            for ((prod, qtyToRestore) in stockRestorations) {
                val currentProd = productDao.getProductById(prod.id) ?: prod
                val oldStock = currentProd.currentStock
                val newStock = oldStock + qtyToRestore
                productDao.update(currentProd.copy(currentStock = newStock).stamped(deviceId))
                stockAdjustmentDao.insertAdjustment(
                    StockAdjustment(
                        globalId = SyncIdentity.newGlobalId(),
                        productId = currentProd.id,
                        oldStock = oldStock,
                        newStock = newStock,
                        difference = qtyToRestore,
                        reason = "Customer Return (Bill: ${sale.billNumber}, Ret: $returnNumber)",
                        createdAt = now,
                        updatedAt = now,
                        isSynced = false
                    ).stamped(deviceId)
                )
            }

            // If sale was UDHAAR and customer is attached, post UdhaarTransaction REVERSAL
            if (sale.paymentMode.equals(PaymentMode.UDHAAR.name, ignoreCase = true) && sale.customerId != null) {
                val customerId = sale.customerId
                val reversalTx = UdhaarTransaction(
                    globalId = SyncIdentity.newGlobalId(),
                    customerId = customerId,
                    saleId = sale.id,
                    type = UdhaarTransactionType.REVERSAL.name,
                    amount = totalRefundPaise,
                    balanceEffect = -totalRefundPaise,
                    note = "Return Reversal: $returnNumber (Bill: ${sale.billNumber})",
                    correctionReason = reason.trim(),
                    actorUid = ledgerActor.actorUid,
                    actorName = ledgerActor.actorName,
                    actorRole = ledgerActor.actorRole,
                    actorDeviceId = ledgerActor.actorDeviceId,
                    mutationVersion = now,
                    mutationDeviceId = ledgerActor.actorDeviceId,
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now
                )
                udhaarDao.insertTransaction(reversalTx)
                customerDao.touchCustomer(customerId, now, ledgerActor.actorDeviceId)
            }

            // Determine if bill is now fully or partially returned
            var isEntireBillReturned = true
            for (lineItem in existingSaleItems) {
                val cumulativeReturnedForItem = rDao.getReturnedQuantityForSaleItem(lineItem.id)
                if (cumulativeReturnedForItem < lineItem.quantity - 1e-4) {
                    isEntireBillReturned = false
                    break
                }
            }

            val updatedPaymentState = if (isEntireBillReturned) {
                PaymentState.REFUNDED
            } else {
                PaymentState.PARTIALLY_REFUNDED
            }

            val newReceived = sale.receivedAmount?.let { prevReceived ->
                maxOf(0L, prevReceived - totalRefundPaise)
            }

            val updatedRows = saleDao.updatePaymentStateIfActive(
                saleId = sale.id,
                paymentState = updatedPaymentState.wireValue,
                receivedAmount = newReceived,
                updatedAt = now,
                mutationDeviceId = deviceId
            )
            check(updatedRows == 1) { "Failed to update sale payment state for sale ${sale.id}" }

            ReturnResult(
                returnId = returnId,
                returnNumber = returnNumber,
                totalRefundAmount = totalRefundPaise,
                updatedPaymentState = updatedPaymentState,
                returnItemCount = finalizedItems.size
            )
        }

        return if (database != null) database.withTransaction { operation() } else operation()
    }

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
        command: CommandMetadata
    ): Long {
        val authorizedCommand = authorize(command, TenantCapability.CHECKOUT)
        val deviceId = authorizedCommand.actor.deviceId
        val stampedSale = sale.stamped(deviceId)
        val stampedItems = items.map { it.stamped(deviceId) }
        val ledgerActor = authorizedCommand.toLedgerActor()
        return saleDao.completeBillCheckout(stampedSale, stampedItems, selectedCustomerId, ledgerActor)
    }

    suspend fun insertSaleWithItems(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerId: Long? = null,
        command: CommandMetadata
    ): Long {
        return completeBillCheckout(sale, items, selectedCustomerId, command)
    }

    suspend fun insertSaleWithNewCustomer(
        sale: Sale,
        items: List<SaleItem>,
        newCustomer: Customer,
        command: CommandMetadata
    ): Long {
        val authorizedCommand = authorize(command, TenantCapability.CHECKOUT)
        val deviceId = authorizedCommand.actor.deviceId
        val stampedSale = sale.stamped(deviceId)
        val stampedItems = items.map { it.stamped(deviceId) }
        return saleDao.completeBillCheckoutWithNewCustomer(
            stampedSale,
            stampedItems,
            newCustomer.stamped(deviceId),
            authorizedCommand.toLedgerActor()
        )
    }

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    
    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)
    suspend fun getCustomerByName(name: String): Customer? = customerDao.getCustomerByName(name)
    suspend fun insertCustomer(customer: Customer, command: CommandMetadata): Long {
        authorize(command, TenantCapability.CATALOG_WRITE)
        return customerDao.insertCustomer(customer.stamped(command.actor.deviceId))
    }

    suspend fun updateCustomer(customer: Customer, command: CommandMetadata) {
        authorize(command, TenantCapability.CATALOG_WRITE)
        customerDao.updateCustomer(customer.stamped(command.actor.deviceId))
    }

    suspend fun deleteCustomer(customer: Customer, command: CommandMetadata) {
        authorize(command, TenantCapability.CATALOG_WRITE)
        customerDao.updateCustomer(customer.stamped(command.actor.deviceId, isDeleted = true))
    }

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
        command: CommandMetadata
    ): Long {
        val authorizedCommand = authorize(command, TenantCapability.LEDGER_RECORD)
        val normalizedActor = LedgerAuditPolicy.requireCanRecord(authorizedCommand.toLedgerActor())
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
        command: CommandMetadata
    ): Long {
        val authorizedCommand = authorize(command, TenantCapability.LEDGER_CORRECTION)
        val normalizedActor = LedgerAuditPolicy.requireCanCorrect(authorizedCommand.toLedgerActor())
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
        command: CommandMetadata
    ): Long {
        val authorizedCommand = authorize(command, TenantCapability.LEDGER_CORRECTION)
        val normalizedActor = LedgerAuditPolicy.requireCanCorrect(authorizedCommand.toLedgerActor())
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

    private suspend fun authorize(
        command: CommandMetadata,
        capability: TenantCapability,
        nowEpochMs: Long = System.currentTimeMillis()
    ): CommandMetadata {
        val (expectedTenant, expectedActor) = authorizationContextProvider?.invoke()
            ?: run {
                val settings = settingsDataStore ?: error("Tenant authorization requires local settings")
                val session = settings.settingsFlow.first().identitySessionOrNull()
                    ?: error("Authenticated session is required")
                val context = settings.getOrCreateTenantDeviceContext(session)
                context.toTenantScope() to PlatformActor(
                    actorId = session.uid,
                    displayName = session.username.ifBlank { session.email }.ifBlank { session.uid },
                    role = session.role,
                    deviceId = context.deviceId
                )
            }
        return TenantAuthorizationPolicy.requireAuthorized(
            command = command,
            expectedTenant = expectedTenant,
            expectedActor = expectedActor,
            capability = capability,
            nowEpochMs = nowEpochMs
        )
    }

    private fun CommandMetadata.toLedgerActor(): LedgerActor = LedgerActor(
        actorUid = actor.actorId,
        actorName = actor.displayName,
        actorRole = actor.role,
        actorDeviceId = actor.deviceId
    ).normalized()

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

    private fun Return.stamped(deviceId: String, isDeleted: Boolean = this.isDeleted): Return {
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

    private fun ReturnItem.stamped(deviceId: String, isDeleted: Boolean = this.isDeleted): ReturnItem {
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

    // Stock Adjustments
    val allStockAdjustments: Flow<List<StockAdjustment>> = stockAdjustmentDao.getAllAdjustments()
    
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>> = 
        stockAdjustmentDao.getAdjustmentsForProduct(productId)

    suspend fun insertStockAdjustment(adjustment: StockAdjustment, command: CommandMetadata): Long {
        authorize(command, TenantCapability.INVENTORY_ADJUSTMENT)
        val normalized = adjustment.copy(
            oldStock = InventoryValidation.validateQuantity(adjustment.oldStock, "Old stock"),
            newStock = InventoryValidation.validateQuantity(adjustment.newStock, "New stock"),
            difference = requireFiniteStockDelta(adjustment.difference),
            reason = InventoryValidation.validateReason(adjustment.reason)
        )
        return stockAdjustmentDao.insertAdjustment(normalized.stamped(command.actor.deviceId))
    }

    /** Corrects a product stock level and logs the audit record atomically. */
    suspend fun adjustProductStock(
        productId: Long,
        actualStockCounted: Double,
        reason: String,
        command: CommandMetadata
    ) {
        val authorizedCommand = authorize(command, TenantCapability.INVENTORY_ADJUSTMENT)
        val validatedReason = InventoryValidation.validateReason(reason)
        val operation: suspend () -> Unit = {
            val product = productDao.getProductById(productId)
            require(product != null && product.isActive && !product.isDeleted) {
                "Stock adjustment requires an active product"
            }
            val validatedStock = if (product.trackStock) {
                InventoryValidation.validateQuantityForUnit(actualStockCounted, "Stock", product.unit)
            } else {
                InventoryValidation.validateQuantity(actualStockCounted, "Stock")
            }
            val oldStock = product.currentStock
            val now = System.currentTimeMillis()
            val deviceId = authorizedCommand.actor.deviceId
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
    suspend fun updateLocalCredential(userId: Long, credentialVerifier: String): Int =
        userDao.updateLocalCredential(userId, credentialVerifier)
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

    suspend fun getUnsyncedReturns(): List<Return> = activeReturnDao.getUnsyncedReturns()
    suspend fun markReturnsSynced(ids: List<Long>) = activeReturnDao.markReturnsSynced(ids)
    suspend fun getUnsyncedReturnItems(): List<ReturnItem> = activeReturnDao.getUnsyncedReturnItems()
    suspend fun markReturnItemsSynced(ids: List<Long>) = activeReturnDao.markReturnItemsSynced(ids)

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
        BusinessRelationshipPolicy.validateRestoreGraph(
            categories = normalizedCategories,
            products = normalizedProducts,
            sales = normalizedSales,
            saleItems = normalizedSaleItems,
            customers = normalizedCustomers,
            udhaarTransactions = normalizedUdhaar,
            stockAdjustments = normalizedAdjustments
        )
        val operation: suspend () -> Unit = {
            categoryDao.clearAllCategories()
            productDao.clearAllProducts()
            saleDao.clearAllSales()
            saleDao.clearAllSaleItems()
            if (returnDao != null || database != null) {
                activeReturnDao.clearAllReturns()
                activeReturnDao.clearAllReturnItems()
            }
            customerDao.clearAllCustomers()
            udhaarDao.clearAllTransactions()
            stockAdjustmentDao.clearAllAdjustments()
            syncOutboxDao().clearAll()

            if (normalizedCategories.isNotEmpty()) categoryDao.insertAllForRestore(normalizedCategories)
            if (normalizedProducts.isNotEmpty()) productDao.insertAllForRestore(normalizedProducts)
            if (normalizedSales.isNotEmpty()) saleDao.insertAllSalesForRestore(normalizedSales)
            if (normalizedSaleItems.isNotEmpty()) saleDao.insertAllSaleItemsForRestore(normalizedSaleItems)
            if (normalizedCustomers.isNotEmpty()) customerDao.insertAllForRestore(normalizedCustomers)
            if (normalizedUdhaar.isNotEmpty()) udhaarDao.insertAllForRestore(normalizedUdhaar)
            if (normalizedAdjustments.isNotEmpty()) stockAdjustmentDao.insertAllForRestore(normalizedAdjustments)
        }
        if (database != null) database.withTransaction { operation() } else operation()
    }

    suspend fun getSyncOutboxSummary(): SyncOutboxSummary {
        val outbox = syncOutboxDao()
        return SyncOutboxSummary(
            pendingCount = outbox.countByState(SyncOutboxState.PENDING),
            inFlightCount = outbox.countByState(SyncOutboxState.IN_FLIGHT),
            retryableCount = outbox.countByState(SyncOutboxState.RETRYABLE),
            deadLetterCount = outbox.countByState(SyncOutboxState.DEAD_LETTER),
            conflictCount = outbox.countConflictDeadLetters(),
            nextRetryAtEpochMs = outbox.getNextRetryAt()
        )
    }

    suspend fun requeueSyncDeadLetters(now: Long = System.currentTimeMillis()): Int =
        syncOutboxDao().requeueDeadLetters(now)

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

    private fun Return.normalizeForRestore(tableName: String): Return = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    private fun ReturnItem.normalizeForRestore(tableName: String): ReturnItem = copy(
        globalId = globalId.ifBlank { SyncIdentity.legacyGlobalId(tableName, id) },
        mutationVersion = if (mutationVersion > 0L) mutationVersion else updatedAt,
        mutationDeviceId = mutationDeviceId.ifBlank { SyncIdentity.LEGACY_DEVICE_ID },
        isSynced = true
    )

    suspend fun getAllSaleItems(): List<SaleItem> = saleDao.getAllSaleItemsList()
}

data class ItemReturnRequest(
    val saleItemId: Long,
    val quantityReturned: Double
)

data class ReturnResult(
    val returnId: Long,
    val returnNumber: String,
    val totalRefundAmount: Long,
    val updatedPaymentState: PaymentState,
    val returnItemCount: Int
)

