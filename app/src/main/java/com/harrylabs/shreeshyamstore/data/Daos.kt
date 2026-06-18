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

    @Query("SELECT * FROM categories WHERE id = :id AND deletedAt IS NULL")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND deletedAt IS NULL LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Update
    suspend fun delete(category: Category)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id AND deletedAt IS NULL")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE id = :id AND deletedAt IS NULL")
    fun getProductByIdFlow(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY name ASC")
    fun getProductsByCategory(categoryId: Long): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :id AND deletedAt IS NULL")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE createdAt >= :start AND createdAt <= :end AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getSalesForDateRange(start: Long, end: Long): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND deletedAt IS NULL")
    fun getSaleItemsForSale(saleId: Long): Flow<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId AND deletedAt IS NULL")
    suspend fun getSaleItemsForSaleList(saleId: Long): List<SaleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(saleItem: SaleItem): Long
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE deletedAt IS NULL AND isActive = 1 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id AND deletedAt IS NULL")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE name = :name AND deletedAt IS NULL LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

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
    fun getTransactionsForCustomer(customerId: Long): Flow<List<UdhaarTransaction>>

    @Query("SELECT * FROM udhaar_transactions WHERE customerId = :customerId AND deletedAt IS NULL")
    suspend fun getTransactionsForCustomerList(customerId: Long): List<UdhaarTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: UdhaarTransaction): Long

    @Update
    suspend fun deleteTransaction(transaction: UdhaarTransaction)
}

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllAdjustments(): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE productId = :productId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: StockAdjustment): Long
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username OR email = :email LIMIT 1")
    suspend fun getUserByUsernameOrEmail(username: String, email: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): User?
}

