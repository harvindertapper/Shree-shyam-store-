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
import com.aistudio.shreeshyamstore.pqwzkb.utils.ShareUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    val salesHistory: StateFlow<List<Sale>> = repository.allSales.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    fun getSaleItems(saleId: Long): Flow<List<SaleItem>> =
        repository.getSaleItemsForSale(saleId)

    fun exportSalesCsv(context: Context, salesToExport: List<Sale>) {
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            val strings = LocaleHelper.getStrings(settings.appLanguage)
            val shopDisplayName = settings.shopName.ifEmpty { strings.defaultShopName }
            ShareUtils.exportSalesCsv(
                context = context,
                sales = salesToExport,
                shopName = shopDisplayName
            )
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
