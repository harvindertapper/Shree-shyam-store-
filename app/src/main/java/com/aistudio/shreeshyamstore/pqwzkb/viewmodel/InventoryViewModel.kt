package com.aistudio.shreeshyamstore.pqwzkb.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.shreeshyamstore.pqwzkb.commerce.CommandMetadata
import com.aistudio.shreeshyamstore.pqwzkb.commerce.InventoryValidation
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.utils.MutationStage
import com.aistudio.shreeshyamstore.pqwzkb.utils.MutationStatus
import com.aistudio.shreeshyamstore.pqwzkb.utils.mutationStageFor
import com.aistudio.shreeshyamstore.pqwzkb.utils.OperatorAction
import com.aistudio.shreeshyamstore.pqwzkb.utils.localizedOperatorGateMessage
import com.aistudio.shreeshyamstore.pqwzkb.data.Category
import com.aistudio.shreeshyamstore.pqwzkb.data.Product
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
import com.aistudio.shreeshyamstore.pqwzkb.data.StockAdjustment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _mutationStatus = MutableStateFlow(MutationStatus())
    val mutationStatus: StateFlow<MutationStatus> = _mutationStatus.asStateFlow()

    private val _mutationInFlight = MutableStateFlow(false)
    val mutationInFlight: StateFlow<Boolean> = _mutationInFlight.asStateFlow()

    private var retryAction: (() -> Unit)? = null

    fun dismissMutationStatus() {
        if (!_mutationInFlight.value) {
            _mutationStatus.value = MutationStatus()
            retryAction = null
        }
    }

    fun retryLastMutation() {
        if (!_mutationInFlight.value) retryAction?.invoke()
    }

    private fun beginMutation(retry: () -> Unit): Boolean {
        if (_mutationInFlight.value) return false
        retryAction = retry
        _mutationInFlight.value = true
        _mutationStatus.value = MutationStatus(MutationStage.VALIDATING)
        return true
    }

    private fun markMutationSavedLocally() {
        _mutationInFlight.value = false
        retryAction = null
        _mutationStatus.value = MutationStatus(
            stage = MutationStage.SAVED_LOCALLY,
            message = LocaleHelper.getStrings(languageProvider()).statusSavedLocallyDetail
        )
    }

    private fun reportMutationFailure(fallback: String, error: Throwable) {
        val stage = mutationStageFor(
            error = error,
            localizedGateMessage = localizedOperatorGateMessage(error, languageProvider())
        )
        val strings = LocaleHelper.getStrings(languageProvider())
        val message = localizedOperatorGateMessage(error, languageProvider()) ?: when (stage) {
            MutationStage.VALIDATION_ERROR -> strings.statusValidationError
            MutationStage.AUTH_ERROR -> strings.statusAuthError
            MutationStage.RETRYABLE_ERROR -> strings.statusRetryableError
            MutationStage.CONFLICT -> strings.statusConflict
            MutationStage.FAILURE -> strings.statusFailure
            else -> fallback
        }
        _mutationInFlight.value = false
        _mutationStatus.value = MutationStatus(
            stage = stage,
            message = message,
            canRetry = stage == MutationStage.RETRYABLE_ERROR
        )
        onError(message)
    }

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
        if (!beginMutation { addCategory(name, onSuccess) }) return
        viewModelScope.launch {
            try {
                val normalizedName = InventoryValidation.validateCategoryName(name)
                require(repository.getCategoryByName(normalizedName) == null) {
                    "Category name already exists"
                }
                _mutationStatus.value = MutationStatus(MutationStage.SAVING_LOCALLY)
                val category = Category(name = normalizedName)
                val insertedId = repository.insertCategory(
                    category = category,
                    command = actionCommandProvider(OperatorAction.CATALOG_WRITE)
                )
                onMutation()
                markMutationSavedLocally()
                onSuccess(category.copy(id = insertedId))
            } catch (error: Exception) {
                reportMutationFailure("Category could not be saved", error)
            }
        }
    }

    fun renameCategory(
        category: Category,
        newName: String,
        onSuccess: () -> Unit = {}
    ) {
        if (!beginMutation { renameCategory(category, newName, onSuccess) }) return
        viewModelScope.launch {
            try {
                val normalizedName = InventoryValidation.validateCategoryName(newName)
                _mutationStatus.value = MutationStatus(MutationStage.SAVING_LOCALLY)
                repository.updateCategory(
                    category = category.copy(
                        name = normalizedName,
                        updatedAt = System.currentTimeMillis()
                    ),
                    command = actionCommandProvider(OperatorAction.CATALOG_WRITE)
                )
                onMutation()
                markMutationSavedLocally()
                onSuccess()
            } catch (error: Exception) {
                reportMutationFailure("Category could not be renamed", error)
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
        if (!beginMutation {
                saveProduct(
                    id, name, categoryId, mrp, sellingPrice, purchasePrice, currentStock,
                    unit, trackStock, lowStockAlertQty, isActive, barcode, onSuccess
                )
            }) return
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
                _mutationStatus.value = MutationStatus(MutationStage.SAVING_LOCALLY)
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
                        command = actionCommandProvider(OperatorAction.CATALOG_WRITE)
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
                markMutationSavedLocally()
                onSuccess()
            } catch (error: Exception) {
                reportMutationFailure("Product could not be saved", error)
            }
        }
    }

    suspend fun getProduct(id: Long): Product? = repository.getProductById(id)

    fun adjustStock(
        productId: Long,
        actualStockCounted: Double,
        reason: String,
        onSuccess: () -> Unit = {}
    ) {
        if (!beginMutation { adjustStock(productId, actualStockCounted, reason, onSuccess) }) return
        viewModelScope.launch {
            try {
                _mutationStatus.value = MutationStatus(MutationStage.SAVING_LOCALLY)
                repository.adjustProductStock(
                    productId = productId,
                    actualStockCounted = actualStockCounted,
                    reason = reason,
                    command = actionCommandProvider(OperatorAction.INVENTORY_ADJUSTMENT)
                )
                onMutation()
                markMutationSavedLocally()
                onSuccess()
            } catch (error: Exception) {
                reportMutationFailure("Stock adjustment could not be saved", error)
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
