package com.aistudio.shreeshyamstore.pqwzkb.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommerceValidation
import com.aistudio.shreeshyamstore.pqwzkb.commerce.LedgerActor
import com.aistudio.shreeshyamstore.pqwzkb.commerce.LedgerAuditPolicy
import com.aistudio.shreeshyamstore.pqwzkb.commerce.PaymentMode
import com.aistudio.shreeshyamstore.pqwzkb.commerce.UdhaarTransactionType
import com.aistudio.shreeshyamstore.pqwzkb.utils.SyncIdentity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND isDeleted = 0 LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Query("SELECT * FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) AND isDeleted = 0 AND id != :excludeId LIMIT 1")
    suspend fun getCategoryByNameExcludingId(name: String, excludeId: Long): Category?

    @Query("SELECT * FROM categories WHERE isSynced = 0")
    suspend fun getUnsyncedCategories(): List<Category>

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM categories WHERE globalId = :globalId LIMIT 1")
    suspend fun getSyncStamp(globalId: String): SyncRecordStamp?

    @Query("UPDATE categories SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markCategoriesSynced(ids: List<Long>)

    @Query("UPDATE categories SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    suspend fun markCategorySyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcodeKey = :barcodeKey AND isDeleted = 0 AND id != :excludeId LIMIT 1")
    suspend fun getActiveProductByBarcodeKey(barcodeKey: String, excludeId: Long): Product?

    @Query("SELECT * FROM products WHERE UPPER(TRIM(barcode)) = :barcodeKey AND isDeleted = 0 AND id != :excludeId LIMIT 1")
    suspend fun getActiveProductByLegacyBarcode(barcodeKey: String, excludeId: Long): Product?

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductByIdFlow(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY name ASC")
    fun getProductsByCategory(categoryId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedProducts(): List<Product>

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM products WHERE globalId = :globalId LIMIT 1")
    suspend fun getSyncStamp(globalId: String): SyncRecordStamp?

    @Query("UPDATE products SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markProductsSynced(ids: List<Long>)

    @Query("UPDATE products SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    suspend fun markProductSyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Dao
abstract class SaleDao {
    @Query("SELECT * FROM sales WHERE isDeleted = 0 ORDER BY createdAt DESC")
    abstract fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    abstract suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT COUNT(*) FROM sales")
    abstract suspend fun countAllSales(): Int

    @Query("SELECT COUNT(*) FROM sales WHERE billNumber = :billNumber")
    abstract suspend fun countSalesByBillNumber(billNumber: String): Int

    @Query("SELECT * FROM sales WHERE createdAt >= :start AND createdAt <= :end AND isDeleted = 0 ORDER BY createdAt DESC")
    abstract fun getSalesForDateRange(start: Long, end: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE isSynced = 0")
    abstract suspend fun getUnsyncedSales(): List<Sale>

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM sales WHERE globalId = :globalId LIMIT 1")
    abstract suspend fun getSaleSyncStamp(globalId: String): SyncRecordStamp?

    @Query("UPDATE sales SET isSynced = 1 WHERE id IN (:ids)")
    abstract suspend fun markSalesSynced(ids: List<Long>)

    @Query("UPDATE sales SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    abstract suspend fun markSaleSyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Query("UPDATE sales SET paymentState = :paymentState, receivedAmount = :receivedAmount, updatedAt = :updatedAt, mutationVersion = :updatedAt, mutationDeviceId = :mutationDeviceId, isSynced = 0 WHERE id = :saleId AND isDeleted = 0")
    abstract suspend fun updatePaymentStateIfActive(
        saleId: Long,
        paymentState: String,
        receivedAmount: Long?,
        updatedAt: Long,
        mutationDeviceId: String
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSale(sale: Sale): Long

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND isDeleted = 0")
    abstract fun getSaleItemsForSale(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    abstract suspend fun getSaleItemsForSaleList(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items")
    abstract suspend fun getAllSaleItemsList(): List<SaleItem>

    @Query("SELECT * FROM sale_items WHERE isSynced = 0")
    abstract suspend fun getUnsyncedSaleItems(): List<SaleItem>

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM sale_items WHERE globalId = :globalId LIMIT 1")
    abstract suspend fun getSaleItemSyncStamp(globalId: String): SyncRecordStamp?

    @Query("UPDATE sale_items SET isSynced = 1 WHERE id IN (:ids)")
    abstract suspend fun markSaleItemsSynced(ids: List<Long>)

    @Query("UPDATE sale_items SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    abstract suspend fun markSaleItemSyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSaleItem(saleItem: SaleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAllSales(sales: List<Sale>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAllSaleItems(items: List<SaleItem>)

    @Query("DELETE FROM sales")
    abstract suspend fun clearAllSales()

    @Query("DELETE FROM sale_items")
    abstract suspend fun clearAllSaleItems()

    // Helper database operations for atomic transactions
    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    abstract suspend fun getProductById(productId: Long): Product?

    @Query("UPDATE products SET currentStock = currentStock - :quantity, updatedAt = :updatedAt, isSynced = 0 WHERE id = :productId AND (trackStock = 0 OR (currentStock >= :quantity AND currentStock >= 0))")
    abstract suspend fun deductProductStockIfAvailable(productId: Long, quantity: Double, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertStockAdjustment(adjustment: StockAdjustment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertUdhaarTransaction(transaction: UdhaarTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCustomer(customer: Customer): Long

    @Query("UPDATE customers SET updatedAt = :updatedAt, mutationVersion = :updatedAt, mutationDeviceId = :mutationDeviceId, isSynced = 0 WHERE id = :customerId")
    abstract suspend fun touchCustomer(customerId: Long, updatedAt: Long, mutationDeviceId: String)

    @Query("SELECT COUNT(*) > 0 FROM customers WHERE id = :customerId AND isDeleted = 0")
    abstract suspend fun hasActiveCustomer(customerId: Long): Boolean

    @Query("SELECT creditLimit FROM customers WHERE id = :customerId AND isDeleted = 0 LIMIT 1")
    abstract suspend fun getCustomerCreditLimit(customerId: Long): Long?

    @Query("SELECT COALESCE(SUM(balanceEffect), 0) FROM udhaar_transactions WHERE customerId = :customerId AND isDeleted = 0")
    abstract suspend fun getCustomerBalance(customerId: Long): Long

    /**
     * Executes atomic bill checkout transaction in local SQLite:
     * 1. Inserts Bill/Sale record
     * 2. Inserts all BillItems/SaleItems line entries
     * 3. Deducts sold quantity from Product.currentStock for tracked products
     * 4. Logs StockAdjustment audit history record
     * 5. If payment method is UDHAAR, logs UdhaarTransaction credit record & touches customer
     */
    @Transaction
    open suspend fun completeBillCheckoutWithNewCustomer(
        sale: Sale,
        items: List<SaleItem>,
        newCustomer: Customer,
        ledgerActor: LedgerActor? = null
    ): Long {
        val customerId = insertCustomer(newCustomer)
        return completeBillCheckout(
            sale = sale.copy(customerId = customerId),
            items = items,
            selectedCustomerId = customerId,
            ledgerActor = ledgerActor
        )
    }

    @Transaction
    open suspend fun completeBillCheckout(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerId: Long? = null,
        ledgerActor: LedgerActor? = null
    ): Long {
        require(items.isNotEmpty()) { "A bill must contain at least one item" }
        require(sale.billNumber.trim().isNotEmpty()) { "Bill number is required" }
        require(countSalesByBillNumber(sale.billNumber) == 0) {
            "Bill number already processed"
        }

        val paymentMode = PaymentMode.parse(sale.paymentMode)
        require(sale.totalAmount >= 0L) {
            "Sale total must be a non-negative amount"
        }

        items.forEach { item ->
            require(item.productId > 0L) { "Sale item must reference a product" }
            require(item.quantity.isFinite() && item.quantity > 0.0) {
                "Sale quantity must be positive"
            }
            require(item.unitPrice >= 0L) {
                "Sale unit price is invalid"
            }
            require(item.lineTotal >= 0L) {
                "Sale line total is invalid"
            }
            require(getProductById(item.productId) != null) {
                "Sale item references a missing product"
            }
            val calculatedLineTotal = CommerceValidation.calculateLineTotal(item.unitPrice, item.quantity)
            require(CommerceValidation.amountsMatch(calculatedLineTotal, item.lineTotal)) {
                "Sale line total does not match its unit price and quantity"
            }
        }

        val calculatedTotal = CommerceValidation.calculateBillTotal(items)
        require(CommerceValidation.amountsMatch(calculatedTotal, sale.totalAmount)) {
            "Sale total does not match its line items"
        }
        val (validatedPaymentState, validatedReceivedAmount) = CommerceValidation.validateCheckoutPayment(
            paymentModeRaw = paymentMode.name,
            totalAmount = calculatedTotal,
            receivedAmount = sale.receivedAmount
        )

        val now = System.currentTimeMillis()
        val validatedLedgerActor = if (paymentMode == PaymentMode.UDHAAR) {
            LedgerAuditPolicy.requireCanRecord(
                ledgerActor ?: throw IllegalArgumentException("Authenticated actor is required for udhaar")
            )
        } else {
            null
        }
        val finalCustomerId = if (paymentMode == PaymentMode.UDHAAR) {
            val customerId = selectedCustomerId ?: sale.customerId
            require(customerId != null && hasActiveCustomer(customerId)) {
                "Udhaar bills require an active customer"
            }
            val creditLimit = getCustomerCreditLimit(customerId)
            require(creditLimit != null && creditLimit >= 0L) {
                "Customer credit limit is invalid"
            }
            val currentBalance = getCustomerBalance(customerId)
            val projectedBalance = currentBalance + calculatedTotal
            require(projectedBalance <= creditLimit) {
                "Udhaar credit limit exceeded"
            }
            customerId
        } else {
            null
        }

        val finalizedSale = sale.copy(
            customerId = finalCustomerId,
            totalAmount = calculatedTotal,
            paymentMode = paymentMode.name,
            paymentState = validatedPaymentState.wireValue,
            receivedAmount = validatedReceivedAmount,
            createdAt = if (sale.createdAt > 0) sale.createdAt else now,
            updatedAt = now,
            mutationVersion = now,
            mutationDeviceId = sale.mutationDeviceId,
            isSynced = false
        )
        val saleId = insertSale(finalizedSale)

        for (item in items) {
            val calculatedLineTotal = CommerceValidation.calculateLineTotal(item.unitPrice, item.quantity)
            val itemToSave = item.copy(
                saleId = saleId,
                unitPrice = CommerceValidation.normalizeUnitPrice(item.unitPrice),
                lineTotal = calculatedLineTotal,
                updatedAt = now,
                mutationVersion = now,
                mutationDeviceId = item.mutationDeviceId,
                isSynced = false
            )
            insertSaleItem(itemToSave)

            val product = getProductById(item.productId)
            require(product != null) { "Sale item references a missing product" }
            if (product.trackStock) {
                require(product.currentStock.isFinite() && product.currentStock >= 0.0) {
                    "Tracked product stock is invalid"
                }
                val updatedRows = deductProductStockIfAvailable(product.id, item.quantity, now)
                require(updatedRows == 1) {
                    "Insufficient stock for product ${product.name}"
                }
                val newStock = product.currentStock - item.quantity
                require(newStock >= 0.0) { "Tracked product stock cannot become negative" }

                insertStockAdjustment(
                    StockAdjustment(
                        globalId = SyncIdentity.newGlobalId(),
                        productId = product.id,
                        oldStock = product.currentStock,
                        newStock = newStock,
                        difference = -item.quantity,
                        reason = "Bill Sale (No: ${sale.billNumber})",
                        isSynced = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }

        if (paymentMode == PaymentMode.UDHAAR && finalCustomerId != null) {
            val actor = validatedLedgerActor ?: error("Validated udhaar actor is missing")
            insertUdhaarTransaction(
                    UdhaarTransaction(
                        globalId = SyncIdentity.newGlobalId(),
                        customerId = finalCustomerId,
                        saleId = saleId,
                        type = UdhaarTransactionType.CREDIT.name,
                        amount = calculatedTotal,
                        balanceEffect = calculatedTotal,
                        note = "Bill No: ${sale.billNumber}",
                        actorUid = actor.actorUid,
                        actorName = actor.actorName,
                        actorRole = actor.actorRole,
                        actorDeviceId = actor.actorDeviceId,
                        mutationVersion = now,
                        mutationDeviceId = actor.actorDeviceId,
                        isSynced = false,
                        createdAt = now,
                        updatedAt = now
                    )
            )
            touchCustomer(finalCustomerId, now, actor.actorDeviceId)
        }

        return saleId
    }
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?

    @Query("SELECT * FROM customers WHERE isSynced = 0")
    suspend fun getUnsyncedCustomers(): List<Customer>

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM customers WHERE globalId = :globalId LIMIT 1")
    suspend fun getSyncStamp(globalId: String): SyncRecordStamp?

    @Query("UPDATE customers SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markCustomersSynced(ids: List<Long>)

    @Query("UPDATE customers SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    suspend fun markCustomerSyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET updatedAt = :updatedAt, mutationVersion = :updatedAt, mutationDeviceId = :mutationDeviceId, isSynced = 0 WHERE id = :customerId")
    suspend fun touchCustomer(customerId: Long, updatedAt: Long, mutationDeviceId: String)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}

@Dao
interface UdhaarDao {
    @Query("SELECT * FROM udhaar_transactions WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<UdhaarTransaction>>

    @Query("SELECT * FROM udhaar_transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<UdhaarTransaction>>

    @Query("SELECT * FROM udhaar_transactions WHERE customerId = :customerId")
    suspend fun getTransactionsForCustomerList(customerId: Long): List<UdhaarTransaction>

    @Query("SELECT COALESCE(SUM(balanceEffect), 0) FROM udhaar_transactions WHERE customerId = :customerId AND isDeleted = 0")
    fun getCustomerBalanceFlow(customerId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(balanceEffect), 0) FROM udhaar_transactions WHERE customerId = :customerId AND isDeleted = 0")
    suspend fun getCustomerBalance(customerId: Long): Long

    @Query("SELECT COALESCE(SUM(balanceEffect), 0) FROM udhaar_transactions WHERE isDeleted = 0")
    fun getTotalUdhaarFlow(): Flow<Long>

    @Query("SELECT COALESCE(SUM(balanceEffect), 0) FROM udhaar_transactions WHERE isDeleted = 0")
    suspend fun getTotalUdhaar(): Long

    @Query("SELECT * FROM udhaar_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<UdhaarTransaction>

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM udhaar_transactions WHERE globalId = :globalId LIMIT 1")
    suspend fun getSyncStamp(globalId: String): SyncRecordStamp?

    @Query("UPDATE udhaar_transactions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markTransactionsSynced(ids: List<Long>)

    @Query("UPDATE udhaar_transactions SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    suspend fun markTransactionSyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Query("SELECT * FROM udhaar_transactions WHERE eventId = :eventId AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveEventById(eventId: String): UdhaarTransaction?

    @Query("SELECT COUNT(*) FROM udhaar_transactions WHERE correctsEventId = :eventId AND isDeleted = 0")
    suspend fun countActiveCorrectionsFor(eventId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: UdhaarTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<UdhaarTransaction>)

    @Query("DELETE FROM udhaar_transactions")
    suspend fun clearAllTransactions()
}

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllAdjustments(): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE productId = :productId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE isSynced = 0")
    suspend fun getUnsyncedAdjustments(): List<StockAdjustment>

    @Query("SELECT id, globalId, mutationVersion, mutationDeviceId FROM stock_adjustments WHERE globalId = :globalId LIMIT 1")
    suspend fun getSyncStamp(globalId: String): SyncRecordStamp?

    @Query("UPDATE stock_adjustments SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAdjustmentsSynced(ids: List<Long>)

    @Query("UPDATE stock_adjustments SET isSynced = 1 WHERE id = :id AND mutationVersion = :mutationVersion AND mutationDeviceId = :mutationDeviceId")
    suspend fun markAdjustmentSyncedIfVersion(id: Long, mutationVersion: Long, mutationDeviceId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: StockAdjustment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(adjustments: List<StockAdjustment>)

    @Query("SELECT * FROM stock_adjustments")
    suspend fun getAllAdjustmentsList(): List<StockAdjustment>

    @Query("DELETE FROM stock_adjustments")
    suspend fun clearAllAdjustments()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username OR email = :email LIMIT 1")
    suspend fun getUserByUsernameOrEmail(username: String, email: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<User>

    @Query("UPDATE users SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markUsersSynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Query("UPDATE users SET passwordHash = :credentialVerifier WHERE id = :userId")
    suspend fun updateLocalCredential(userId: Long, credentialVerifier: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<User>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}

// Alias for BillDao to support direct POS billing terminology
typealias BillDao = SaleDao



@Dao
interface ShopProfileDao {
    @Query("SELECT * FROM shop_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): ShopProfile?

    @Query("SELECT * FROM shop_profiles WHERE isDeleted = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveProfile(): ShopProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ShopProfile)

    @Query("UPDATE shop_profiles SET isSynced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("DELETE FROM shop_profiles")
    suspend fun clearAll()
}
