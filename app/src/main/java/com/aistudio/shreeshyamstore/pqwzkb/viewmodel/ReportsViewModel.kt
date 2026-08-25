package com.aistudio.shreeshyamstore.pqwzkb.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.shreeshyamstore.pqwzkb.data.Sale
import com.aistudio.shreeshyamstore.pqwzkb.data.SaleItem
import com.aistudio.shreeshyamstore.pqwzkb.data.SettingsDataStore
import com.aistudio.shreeshyamstore.pqwzkb.data.ShopRepository
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.utils.SalesExportResult
import com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Read/reporting boundary for sales history and report exports.
 *
 * Navigation, identity/session state, billing mutations, and invoice sharing
 * remain in [ShopViewModel] until their own focused extraction slices land.
 */
class ReportsViewModel(
    private val repository: ShopRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    private val _salesHistory = MutableStateFlow<List<Sale>>(emptyList())
    val salesHistory: StateFlow<List<Sale>> = _salesHistory

    private val _isSalesHistoryLoading = MutableStateFlow(true)
    val isSalesHistoryLoading: StateFlow<Boolean> = _isSalesHistoryLoading

    private val _salesHistoryHasError = MutableStateFlow(false)
    val salesHistoryHasError: StateFlow<Boolean> = _salesHistoryHasError

    private var salesHistoryJob: Job? = null

    init {
        refreshSalesHistory()
    }

    fun refreshSalesHistory() {
        salesHistoryJob?.cancel()
        _isSalesHistoryLoading.value = true
        _salesHistoryHasError.value = false
        salesHistoryJob = viewModelScope.launch {
            repository.allSales
                .catch {
                    _isSalesHistoryLoading.value = false
                    _salesHistoryHasError.value = true
                    emit(emptyList())
                }
                .collect { sales ->
                    _salesHistory.value = sales
                    _isSalesHistoryLoading.value = false
                    _salesHistoryHasError.value = false
                }
        }
    }

    fun getSaleItems(saleId: Long): Flow<List<SaleItem>> =
        repository.getSaleItemsForSale(saleId)

    fun exportSalesCsv(
        context: Context,
        salesToExport: List<Sale>,
        onResult: (SalesExportResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val settings = settingsDataStore.settingsFlow.first()
                val strings = LocaleHelper.getStrings(settings.appLanguage)
                val shopDisplayName = settings.shopName.ifEmpty { strings.defaultShopName }
                val result = ShareUtils.exportSalesCsv(
                    context = context,
                    sales = salesToExport,
                    shopName = shopDisplayName
                )
                onResult(result)
            } catch (_: Exception) {
                onResult(SalesExportResult.FAILED)
            }
        }
    }
}

class ReportsViewModelFactory(
    private val repository: ShopRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportsViewModel(repository, settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
