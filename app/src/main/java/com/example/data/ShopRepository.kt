package com.example.data

import androidx.room.withTransaction
import com.example.commerce.CommerceValidation
import com.example.commerce.UdhaarTransactionType
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
    private val shopProfileDao: ShopProfileDao? = null
) {
    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    
    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)
    suspend fun getCategoryByName(name: String): Category? = categoryDao.getCategoryByName(name)
    suspend fun insertCategory(category: Category): Long = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    
    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)
    fun getProductByIdFlow(id: Long): Flow<Product?> = productDao.getProductByIdFlow(id)
    fun getProductsByCategory(categoryId: Long): Flow<List<Product>> = productDao.getProductsByCategory(categoryId)
    
    suspend fun insertProduct(product: Product): Long = productDao.insert(product)
    suspend fun updateProduct(product: Product) = productDao.update(product)

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
        selectedCustomerId: Long? = null
    ): Long {
        return saleDao.completeBillCheckout(sale, items, selectedCustomerId)
    }

    suspend fun insertSaleWithItems(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerId: Long? = null
    ): Long {
        return completeBillCheckout(sale, items, selectedCustomerId)
    }

    suspend fun insertSaleWithNewCustomer(
        sale: Sale,
        items: List<SaleItem>,
        newCustomer: Customer
    ): Long {
        return saleDao.completeBillCheckoutWithNewCustomer(sale, items, newCustomer)
    }

    // Customers
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    
    suspend fun getCustomerById(id: Long): Customer? = customerDao.getCustomerById(id)
    suspend fun getCustomerByName(name: String): Customer? = customerDao.getCustomerByName(name)
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    // Udhaar
    val allUdhaarTransactions: Flow<List<UdhaarTransaction>> = udhaarDao.getAllTransactions()
    
    fun getTransactionsForCustomer(customerId: Long): Flow<List<UdhaarTransaction>> = 
        udhaarDao.getTransactionsForCustomer(customerId)

    suspend fun getTransactionsForCustomerList(customerId: Long): List<UdhaarTransaction> = 
        udhaarDao.getTransactionsForCustomerList(customerId)

    fun getCustomerBalanceFlow(customerId: Long): Flow<Double> =
        udhaarDao.getCustomerBalanceFlow(customerId)

    suspend fun getCustomerBalance(customerId: Long): Double =
        udhaarDao.getCustomerBalance(customerId)

    fun getTotalUdhaarFlow(): Flow<Double> =
        udhaarDao.getTotalUdhaarFlow()

    suspend fun getTotalUdhaar(): Double =
        udhaarDao.getTotalUdhaar()

    suspend fun insertUdhaarTransaction(transaction: UdhaarTransaction): Long =
        udhaarDao.insertTransaction(transaction)

    suspend fun recordUdhaarPayment(customerId: Long, amount: Double, note: String?): Long {
        val operation: suspend () -> Long = {
            require(amount.isFinite() && amount > 0.0) {
                "Payment amount must be finite and positive"
            }
            val customer = customerDao.getCustomerById(customerId)
            require(customer != null && !customer.isDeleted) {
                "Payment requires an active customer"
            }
            udhaarDao.insertTransaction(
                UdhaarTransaction(
                    customerId = customerId,
                    type = UdhaarTransactionType.PAYMENT.name,
                    amount = CommerceValidation.roundCurrency(amount),
                    note = note?.trim()?.ifEmpty { "Payment received" } ?: "Payment received",
                    isSynced = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return database?.withTransaction { operation() } ?: operation()
    }

    suspend fun deleteUdhaarTransaction(transaction: UdhaarTransaction) =
        udhaarDao.deleteTransaction(transaction)

    // Stock Adjustments
    val allStockAdjustments: Flow<List<StockAdjustment>> = stockAdjustmentDao.getAllAdjustments()
    
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>> = 
        stockAdjustmentDao.getAdjustmentsForProduct(productId)

    suspend fun insertStockAdjustment(adjustment: StockAdjustment): Long {
        return stockAdjustmentDao.insertAdjustment(adjustment)
    }

    /** Corrects a product stock level and logs the audit record atomically. */
    suspend fun adjustProductStock(productId: Long, actualStockCounted: Double, reason: String) {
        require(actualStockCounted >= 0.0) { "Stock cannot be negative" }
        val operation: suspend () -> Unit = operation@{
            val product = productDao.getProductById(productId) ?: return@operation
            val oldStock = product.currentStock
            val now = System.currentTimeMillis()
            productDao.update(
                product.copy(
                    currentStock = actualStockCounted,
                    updatedAt = now,
                    isSynced = false
                )
            )
            stockAdjustmentDao.insertAdjustment(
                StockAdjustment(
                    productId = productId,
                    oldStock = oldStock,
                    newStock = actualStockCounted,
                    difference = actualStockCounted - oldStock,
                    reason = reason.trim().ifEmpty { "Manual stock adjustment" },
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        if (database != null) database.withTransaction { operation() } else operation()
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
        val operation: suspend () -> Unit = {
            categoryDao.clearAllCategories()
            productDao.clearAllProducts()
            saleDao.clearAllSales()
            saleDao.clearAllSaleItems()
            customerDao.clearAllCustomers()
            udhaarDao.clearAllTransactions()
            stockAdjustmentDao.clearAllAdjustments()

            if (categoriesList.isNotEmpty()) categoryDao.insertAll(categoriesList)
            if (productsList.isNotEmpty()) productDao.insertAll(productsList)
            if (salesList.isNotEmpty()) saleDao.insertAllSales(salesList)
            if (saleItemsList.isNotEmpty()) saleDao.insertAllSaleItems(saleItemsList)
            if (customersList.isNotEmpty()) customerDao.insertAll(customersList)
            if (udhaarTxsList.isNotEmpty()) udhaarDao.insertAll(udhaarTxsList)
            if (adjustmentsList.isNotEmpty()) stockAdjustmentDao.insertAll(adjustmentsList)
        }
        if (database != null) database.withTransaction { operation() } else operation()
    }

    suspend fun getAllSaleItems(): List<SaleItem> = saleDao.getAllSaleItemsList()
}

