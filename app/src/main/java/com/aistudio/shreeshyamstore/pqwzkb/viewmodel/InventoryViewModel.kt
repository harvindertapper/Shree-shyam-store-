package com.aistudio.shreeshyamstore.pqwzkb.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    private val onMutation: () -> Unit
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

    fun addCategory(name: String) {
        viewModelScope.launch {
            val normalizedName = name.trim()
            if (normalizedName.isNotEmpty() && repository.getCategoryByName(normalizedName) == null) {
                repository.insertCategory(Category(name = normalizedName))
                onMutation()
            }
        }
    }

    fun renameCategory(category: Category, newName: String) {
        viewModelScope.launch {
            val normalizedName = newName.trim()
            if (normalizedName.isNotEmpty()) {
                repository.updateCategory(
                    category.copy(
                        name = normalizedName,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                onMutation()
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
        barcode: String = ""
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (id == 0L) {
                val product = Product(
                    name = name.trim(),
                    categoryId = categoryId,
                    mrp = mrp,
                    sellingPrice = sellingPrice,
                    purchasePrice = purchasePrice,
                    currentStock = currentStock,
                    unit = unit.trim().ifEmpty { "pcs" },
                    trackStock = trackStock,
                    lowStockAlertQty = lowStockAlertQty,
                    barcode = barcode.trim(),
                    isActive = isActive,
                    createdAt = now,
                    updatedAt = now
                )
                val newProductId = repository.insertProduct(product)
                if (trackStock && currentStock > 0.0) {
                    repository.insertStockAdjustment(
                        StockAdjustment(
                            productId = newProductId,
                            oldStock = 0.0,
                            newStock = currentStock,
                            difference = currentStock,
                            reason = "Opening stock entry",
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                }
            } else {
                val existing = repository.getProductById(id)
                if (existing != null) {
                    val finalStock = if (trackStock) currentStock else existing.currentStock
                    repository.updateProduct(
                        existing.copy(
                            name = name.trim(),
                            categoryId = categoryId,
                            mrp = mrp,
                            sellingPrice = sellingPrice,
                            purchasePrice = purchasePrice,
                            currentStock = finalStock,
                            unit = unit.trim().ifEmpty { existing.unit },
                            trackStock = trackStock,
                            lowStockAlertQty = lowStockAlertQty,
                            barcode = barcode.trim(),
                            isActive = isActive,
                            updatedAt = now
                        )
                    )
                    if (trackStock && currentStock != existing.currentStock) {
                        repository.insertStockAdjustment(
                            StockAdjustment(
                                productId = id,
                                oldStock = existing.currentStock,
                                newStock = currentStock,
                                difference = currentStock - existing.currentStock,
                                reason = "Manual correction during edit",
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    }
                }
            }
            onMutation()
        }
    }

    suspend fun getProduct(id: Long): Product? = repository.getProductById(id)

    fun adjustStock(productId: Long, actualStockCounted: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustProductStock(productId, actualStockCounted, reason)
            onMutation()
        }
    }

    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>> =
        repository.getAdjustmentsForProduct(productId)
}

class InventoryViewModelFactory(
    private val repository: ShopRepository,
    private val onMutation: () -> Unit
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository, onMutation) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
