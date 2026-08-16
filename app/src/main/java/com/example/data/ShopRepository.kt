package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class ShopRepository(
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val customerDao: CustomerDao,
    private val udhaarDao: UdhaarDao,
    private val stockAdjustmentDao: StockAdjustmentDao,
    private val userDao: UserDao,
    private val database: AppDatabase? = null
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
     * Executes the major invoice lock transaction:
     * 1. Saves invoice (Sale)
     * 2. Saves line items (SaleItems)
     * 3. Subtracts stock for tracked items
     * 4. Logs a Stock Adjustment for tracking history
     * 5. Spawns an Udhaar CREDIT record if payment is selected as UDHAAR.
     * Guaranteed atomic via Room database transaction.
     */
    suspend fun insertSaleWithItems(
        sale: Sale,
        items: List<SaleItem>,
        selectedCustomerId: Long? = null
    ): Long {
        val block: suspend () -> Long = {
            val now = System.currentTimeMillis()
            // 1. Insert Sale
            val finalCustomerId = if (sale.paymentMode == "UDHAAR") selectedCustomerId else null
            val finalizedSale = sale.copy(
                customerId = finalCustomerId,
                createdAt = if (sale.createdAt > 0) sale.createdAt else now,
                updatedAt = now,
                isSynced = false
            )
            val saleId = saleDao.insertSale(finalizedSale)

            // 2. Loop and save each item
            for (item in items) {
                val itemToSave = item.copy(
                    saleId = saleId,
                    updatedAt = now,
                    isSynced = false
                )
                saleDao.insertSaleItem(itemToSave)

                // 3. Stock handling
                val product = productDao.getProductById(item.productId)
                if (product != null && product.trackStock) {
                    val oldStock = product.currentStock
                    val newStock = oldStock - item.quantity
                    
                    // Update product stock
                    val updatedProduct = product.copy(
                        currentStock = newStock,
                        updatedAt = now,
                        isSynced = false
                    )
                    productDao.update(updatedProduct)

                    // Create stock adjustment history record
                    val adj = StockAdjustment(
                        productId = product.id,
                        oldStock = oldStock,
                        newStock = newStock,
                        difference = -item.quantity,
                        reason = "Bill Sale (No: ${sale.billNumber})",
                        isSynced = false,
                        createdAt = now,
                        updatedAt = now
                    )
                    stockAdjustmentDao.insertAdjustment(adj)
                }
            }

            // 4. Udhaar Transaction handling if payment mode is UDHAAR
            if (sale.paymentMode == "UDHAAR" && finalCustomerId != null) {
                val udhaarTx = UdhaarTransaction(
                    customerId = finalCustomerId,
                    saleId = saleId,
                    type = "CREDIT",
                    amount = sale.totalAmount,
                    note = "Bill No: ${sale.billNumber}",
                    isSynced = false,
                    createdAt = now,
                    updatedAt = now
                )
                udhaarDao.insertTransaction(udhaarTx)
            }

            saleId
        }

        return if (database != null) {
            database.withTransaction {
                block()
            }
        } else {
            block()
        }
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

    suspend fun deleteUdhaarTransaction(transaction: UdhaarTransaction) = 
        udhaarDao.deleteTransaction(transaction)

    // Stock Adjustments
    val allStockAdjustments: Flow<List<StockAdjustment>> = stockAdjustmentDao.getAllAdjustments()
    
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>> = 
        stockAdjustmentDao.getAdjustmentsForProduct(productId)

    suspend fun insertStockAdjustment(adjustment: StockAdjustment): Long {
        return stockAdjustmentDao.insertAdjustment(adjustment)
    }

    /**
     * Corrects a product stock level manually and logs history
     */
    suspend fun adjustProductStock(productId: Long, actualStockCounted: Double, reason: String) {
        val product = productDao.getProductById(productId) ?: return
        val oldStock = product.currentStock
        val diff = actualStockCounted - oldStock
        val now = System.currentTimeMillis()
        
        val updatedProduct = product.copy(
            currentStock = actualStockCounted,
            updatedAt = now,
            isSynced = false
        )
        productDao.update(updatedProduct)

        val adjustment = StockAdjustment(
            productId = productId,
            oldStock = oldStock,
            newStock = actualStockCounted,
            difference = diff,
            reason = reason,
            isSynced = false,
            createdAt = now,
            updatedAt = now
        )
        stockAdjustmentDao.insertAdjustment(adjustment)
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

    suspend fun clearAllLocalTables() {
        categoryDao.clearAllCategories()
        productDao.clearAllProducts()
        saleDao.clearAllSales()
        saleDao.clearAllSaleItems()
        customerDao.clearAllCustomers()
        udhaarDao.clearAllTransactions()
        stockAdjustmentDao.clearAllAdjustments()
        userDao.clearAllUsers()
    }

    suspend fun insertRestoredData(
        categoriesList: List<Category>,
        productsList: List<Product>,
        salesList: List<Sale>,
        saleItemsList: List<SaleItem>,
        customersList: List<Customer>,
        udhaarTxsList: List<UdhaarTransaction>,
        adjustmentsList: List<StockAdjustment>,
        usersList: List<User>
    ) {
        if (categoriesList.isNotEmpty()) categoryDao.insertAll(categoriesList)
        if (productsList.isNotEmpty()) productDao.insertAll(productsList)
        if (salesList.isNotEmpty()) saleDao.insertAllSales(salesList)
        if (saleItemsList.isNotEmpty()) saleDao.insertAllSaleItems(saleItemsList)
        if (customersList.isNotEmpty()) customerDao.insertAll(customersList)
        if (udhaarTxsList.isNotEmpty()) udhaarDao.insertAll(udhaarTxsList)
        if (adjustmentsList.isNotEmpty()) stockAdjustmentDao.insertAll(adjustmentsList)
        if (usersList.isNotEmpty()) userDao.insertAll(usersList)
    }

    suspend fun getAllSaleItems(): List<SaleItem> = saleDao.getAllSaleItemsList()
}

