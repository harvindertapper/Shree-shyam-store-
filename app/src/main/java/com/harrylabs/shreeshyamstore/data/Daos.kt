package com.harrylabs.shreeshyamstore.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL AND isActive = 1 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE localUuid = :uuid AND deletedAt IS NULL")
    suspend fun getCategoryById(uuid: String): Category?

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY name ASC")
    suspend fun getAllCategoriesOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE lower(name) = lower(:name) AND deletedAt IS NULL LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Update
    suspend fun delete(category: Category)
}

@Dao
interface SyncOutboxDao {
    @Query(
        """
        SELECT * FROM sync_outbox_operations
        WHERE shopId = :shopId
          AND (syncStatus = :pendingStatus OR syncStatus = :failedStatus)
        ORDER BY createdAt ASC
        """
    )
    suspend fun getPendingOrFailedOperations(
        shopId: String,
        pendingStatus: String = SyncStatus.PENDING,
        failedStatus: String = SyncStatus.FAILED
    ): List<SyncOutboxOperation>

    @Query(
        """
        SELECT COUNT(*) FROM sync_outbox_operations
        WHERE shopId = :shopId
          AND (syncStatus = :pendingStatus OR syncStatus = :failedStatus)
        """
    )
    suspend fun getPendingOrFailedOperationCount(
        shopId: String,
        pendingStatus: String = SyncStatus.PENDING,
        failedStatus: String = SyncStatus.FAILED
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncOutboxOperation)

    @Query(
        """
        UPDATE sync_outbox_operations
        SET syncStatus = :status,
            retryCount = retryCount + 1,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE shopId = :shopId
          AND (syncStatus = :pendingStatus OR syncStatus = :failedStatus)
        """
    )
    suspend fun markOperationsFailed(
        shopId: String,
        status: String,
        lastError: String,
        updatedAt: Long,
        pendingStatus: String = SyncStatus.PENDING,
        failedStatus: String = SyncStatus.FAILED
    )

    @Query(
        """
        DELETE FROM sync_outbox_operations
        WHERE shopId = :shopId
          AND (syncStatus = :pendingStatus OR syncStatus = :failedStatus)
        """
    )
    suspend fun clearPendingAndFailedOperations(
        shopId: String,
        pendingStatus: String = SyncStatus.PENDING,
        failedStatus: String = SyncStatus.FAILED
    )
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE localUuid = :uuid AND deletedAt IS NULL")
    suspend fun getProductById(uuid: String): Product?

    @Query("SELECT * FROM products WHERE deletedAt IS NULL ORDER BY name ASC")
    suspend fun getAllProductsOnce(): List<Product>

    @Query("SELECT * FROM products WHERE localUuid = :uuid AND deletedAt IS NULL")
    fun getProductByIdFlow(uuid: String): Flow<Product?>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY name ASC")
    fun getProductsByCategory(categoryId: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE localUuid = :uuid AND deletedAt IS NULL")
    suspend fun getSaleById(uuid: String): Sale?

    @Query("SELECT * FROM sales WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllSalesOnce(): List<Sale>

    @Query("SELECT * FROM sales WHERE createdAt >= :start AND createdAt <= :end AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getSalesForDateRange(start: Long, end: Long): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(sales: List<Sale>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND deletedAt IS NULL")
    fun getSaleItemsForSale(saleId: String): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND deletedAt IS NULL")
    suspend fun getSaleItemsForSaleList(saleId: String): List<SaleItem>

    @Query("SELECT * FROM sale_items WHERE deletedAt IS NULL")
    suspend fun getAllSaleItemsOnce(): List<SaleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(saleItem: SaleItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSaleItems(saleItems: List<SaleItem>)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE deletedAt IS NULL AND isActive = 1 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE localUuid = :uuid AND deletedAt IS NULL")
    suspend fun getCustomerById(uuid: String): Customer?

    @Query("SELECT * FROM customers WHERE deletedAt IS NULL ORDER BY name ASC")
    suspend fun getAllCustomersOnce(): List<Customer>

    @Query("SELECT * FROM customers WHERE name = :name AND deletedAt IS NULL LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Update
    suspend fun deleteCustomer(customer: Customer)
}

@Dao
interface UdhaarDao {
    @Query("SELECT * FROM udhaar_transactions WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<UdhaarTransaction>>

    @Query("SELECT * FROM udhaar_transactions WHERE customerId = :customerId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getTransactionsForCustomer(customerId: String): Flow<List<UdhaarTransaction>>

    @Query("SELECT * FROM udhaar_transactions WHERE customerId = :customerId AND deletedAt IS NULL")
    suspend fun getTransactionsForCustomerList(customerId: String): List<UdhaarTransaction>

    @Query("SELECT * FROM udhaar_transactions WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllTransactionsOnce(): List<UdhaarTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: UdhaarTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<UdhaarTransaction>)

    @Update
    suspend fun deleteTransaction(transaction: UdhaarTransaction)
}

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllAdjustments(): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE productId = :productId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAdjustmentsForProduct(productId: String): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllAdjustmentsOnce(): List<StockAdjustment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: StockAdjustment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAdjustments(adjustments: List<StockAdjustment>)
}
