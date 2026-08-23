package com.aistudio.shreeshyamstore.pqwzkb.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.InventoryValidation
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperatorAction
import com.aistudio.shreeshyamstore.pqwzkb.utils.localizedOperatorGateMessage
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
import com.aistudio.shreeshyamstore.pqwzkb.data.StockAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Catalog and inventory orchestration boundary.
 *
 * Checkout/cart, quick-add billing behavior, navigation, exports, and sync
 * orchestration remain on [ShopViewModel] until their own extraction slices.
 * All persistence and stock-audit semantics continue to live in ShopRepository.
 */
class InventoryViewModel(
    private val repository: ShopRepository,
    private val commandProvider: suspend () -> CommandMetadata,
    private val onMutation: () -> Unit,
    private val onError: (String) -> Unit = {},
    private val actionCommandProvider: suspend (OperatorAction) -> CommandMetadata = { commandProvider() },
    private val languageProvider: () -> AppLanguage = { AppLanguage.ENGLISH }
) : ViewModel() {
    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val products: StateFlow<List<Product>> = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun addCategory(name: String, onSuccess: (Category) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val normalizedName = InventoryValidation.validateCategoryName(name)
                require(repository.getCategoryByName(normalizedName) == null) {
                    "Category name already exists"
                }
                val category = Category(name = normalizedName)
                val insertedId = repository.insertCategory(
                    category = category,
                    command = actionCommandProvider(OperatorAction.CATALOG_WRITE)
                )
                onMutation()
                onSuccess(category.copy(id = insertedId))
            } catch (error: Exception) {
                onError(safeMutationMessage("Category could not be saved", error))
            }
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            try {
                val normalizedName = InventoryValidation.validateCategoryName(newName)
                repository.updateCategory(
                    category = category.copy(
                        name = normalizedName,
                        updatedAt = System.currentTimeMillis()
                    ),
                    command = actionCommandProvider(OperatorAction.CATALOG_WRITE)
                )
                onMutation()
            } catch (error: Exception) {
                onError(safeMutationMessage("Category could not be renamed", error))
            }
        }
    }

    fun saveProduct(
        id: Long,
        name: String,
        categoryId: Long,
        mrp: Long,
        sellingPrice: Long?,
        purchasePrice: Long?,
        currentStock: Double,
        unit: String = "pcs",
        trackStock: Boolean,
        lowStockAlertQty: Double,
        isActive: Boolean,
        barcode: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val normalizedName = InventoryValidation.validateProductName(name)
                val normalizedMrp = InventoryValidation.validateProductMoney(mrp, "MRP")
                val normalizedSellingPrice = InventoryValidation.validateOptionalMoney(sellingPrice, "Selling price")
                val normalizedPurchasePrice = InventoryValidation.validateOptionalMoney(purchasePrice, "Purchase price")
                val normalizedStock = InventoryValidation.validateQuantity(currentStock, "Current stock")
                val normalizedAlertQty = InventoryValidation.validateQuantity(lowStockAlertQty, "Low-stock alert quantity")
                val normalizedUnit = InventoryValidation.validateUnit(unit)
                val normalizedBarcode = InventoryValidation.normalizeBarcode(barcode)
                require(repository.isBarcodeAvailable(normalizedBarcode.orEmpty(), id)) {
                    "Barcode already belongs to another active product"
                }

                val now = System.currentTimeMillis()
                if (id == 0L) {
                    val product = Product(
                        name = normalizedName,
                        categoryId = categoryId,
                        mrp = normalizedMrp,
                        sellingPrice = normalizedSellingPrice,
                        purchasePrice = normalizedPurchasePrice,
                        currentStock = normalizedStock,
                        unit = normalizedUnit,
                        trackStock = trackStock,
                        lowStockAlertQty = normalizedAlertQty,
                        barcode = barcode.trim(),
                        barcodeKey = normalizedBarcode,
                        isActive = isActive,
                        createdAt = now,
                        updatedAt = now
                    )
                    repository.insertProductWithOpeningStock(
                        product = product,
                        openingStock = normalizedStock,
                        createdAt = now,
                        command = commandProvider()
                    )
                } else {
                    val existing = repository.getProductById(id)
                        ?: error("Product was not found")
                    val finalStock = if (trackStock) normalizedStock else existing.currentStock
                    repository.updateProductWithStockAdjustment(
                        product = existing.copy(
                            name = normalizedName,
                            categoryId = categoryId,
                            mrp = normalizedMrp,
                            sellingPrice = normalizedSellingPrice,
                            purchasePrice = normalizedPurchasePrice,
                            currentStock = finalStock,
                            unit = normalizedUnit,
                            trackStock = trackStock,
                            lowStockAlertQty = normalizedAlertQty,
                            barcode = barcode.trim(),
                            barcodeKey = normalizedBarcode,
                            isActive = isActive,
                            updatedAt = now
                        ),
                        oldStock = existing.currentStock,
                        newStock = finalStock,
                        reason = "Manual correction during edit",
                        createdAt = now,
                        command = actionCommandProvider(OperatorAction.CATALOG_WRITE)
                    )
                }
                onMutation()
                onSuccess()
            } catch (error: Exception) {
                onError(safeMutationMessage("Product could not be saved", error))
            }
        }
    }

    private fun safeMutationMessage(fallback: String, error: Throwable): String {
        localizedOperatorGateMessage(error, languageProvider())?.let { return it }
        val message = error.message?.trim().orEmpty()
        val strings = com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
            .getStrings(languageProvider())
        return when {
            message.contains("Authenticated actor is required", ignoreCase = true) ->
                strings.actionSignInRequired
            message.contains("not authorized", ignoreCase = true) ->
                strings.actionPermissionDenied
            message.contains("barcode", ignoreCase = true) ||
                message.contains("category", ignoreCase = true) ||
                message.contains("stock", ignoreCase = true) ||
                message.contains("price", ignoreCase = true) ||
                message.contains("name", ignoreCase = true) -> message
            else -> fallback
        }
    }

    suspend fun getProduct(id: Long): Product? = repository.getProductById(id)

    fun adjustStock(productId: Long, actualStockCounted: Double, reason: String) {
        viewModelScope.launch {
            try {
                repository.adjustProductStock(
                    productId = productId,
                    actualStockCounted = actualStockCounted,
                    reason = reason,
                    command = actionCommandProvider(OperatorAction.INVENTORY_ADJUSTMENT)
                )
                onMutation()
            } catch (error: IllegalArgumentException) {
                onError(error.message ?: "Stock adjustment could not be saved")
            } catch (_: Exception) {
                onError("Stock adjustment could not be saved")
            }
        }
    }

    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>> =
        repository.getAdjustmentsForProduct(productId)
}

class InventoryViewModelFactory(
    private val repository: ShopRepository,
    private val commandProvider: suspend () -> CommandMetadata,
    private val onMutation: () -> Unit,
    private val onError: (String) -> Unit = {},
    private val actionCommandProvider: suspend (OperatorAction) -> CommandMetadata = { commandProvider() },
    private val languageProvider: () -> AppLanguage = { AppLanguage.ENGLISH }
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(
                repository,
                commandProvider,
                onMutation,
                onError,
                actionCommandProvider,
                languageProvider
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
