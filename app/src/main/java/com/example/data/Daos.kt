package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Query("SELECT * FROM categories WHERE isSynced = 0")
    suspend fun getUnsyncedCategories(): List<Category>

    @Query("UPDATE categories SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markCategoriesSynced(ids: List<Long>)

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

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductByIdFlow(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY name ASC")
    fun getProductsByCategory(categoryId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isSynced = 0")
    suspend fun getUnsyncedProducts(): List<Product>

    @Query("UPDATE products SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markProductsSynced(ids: List<Long>)

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
interface SaleDao {
    @Query("SELECT * FROM sales WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE createdAt >= :start AND createdAt <= :end AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getSalesForDateRange(start: Long, end: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<Sale>

    @Query("UPDATE sales SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSalesSynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND isDeleted = 0")
    fun getSaleItemsForSale(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsForSaleList(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsList(): List<SaleItem>

    @Query("SELECT * FROM sale_items WHERE isSynced = 0")
    suspend fun getUnsyncedSaleItems(): List<SaleItem>

    @Query("UPDATE sale_items SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markSaleItemsSynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(saleItem: SaleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSales(sales: List<Sale>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSaleItems(items: List<SaleItem>)

    @Query("DELETE FROM sales")
    suspend fun clearAllSales()

    @Query("DELETE FROM sale_items")
    suspend fun clearAllSaleItems()
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

    @Query("UPDATE customers SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markCustomersSynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<Customer>)

    @Update
    suspend fun updateCustomer(customer: Customer)

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

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0.0) FROM udhaar_transactions WHERE customerId = :customerId AND isDeleted = 0")
    fun getCustomerBalanceFlow(customerId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0.0) FROM udhaar_transactions WHERE customerId = :customerId AND isDeleted = 0")
    suspend fun getCustomerBalance(customerId: Long): Double

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0.0) FROM udhaar_transactions WHERE isDeleted = 0")
    fun getTotalUdhaarFlow(): Flow<Double>

    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0.0) FROM udhaar_transactions WHERE isDeleted = 0")
    suspend fun getTotalUdhaar(): Double

    @Query("SELECT * FROM udhaar_transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<UdhaarTransaction>

    @Query("UPDATE udhaar_transactions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markTransactionsSynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: UdhaarTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<UdhaarTransaction>)

    @Delete
    suspend fun deleteTransaction(transaction: UdhaarTransaction)

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

    @Query("UPDATE stock_adjustments SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAdjustmentsSynced(ids: List<Long>)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<User>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}

