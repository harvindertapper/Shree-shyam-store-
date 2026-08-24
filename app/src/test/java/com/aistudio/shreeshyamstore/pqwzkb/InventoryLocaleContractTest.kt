package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import org.junit.Assert.assertFalse
import org.junit.Test

class InventoryLocaleContractTest {
    @Test
    fun issue53InventoryStringsExistInBothLanguages() {
        val hindi = LocaleHelper.getStrings(AppLanguage.HINDI)
        val english = LocaleHelper.getStrings(AppLanguage.ENGLISH)

        listOf(hindi, english).forEach { strings ->
            inventoryStrings(strings).forEach { value ->
                assertFalse("Issue #53 inventory copy must not be blank", value.isBlank())
            }
        }
    }

    private fun inventoryStrings(strings: AppStrings): List<String> = listOf(
        strings.productFormBack,
        strings.productFormBarcodeLabel,
        strings.productFormBarcodePlaceholder,
        strings.productFormScanBarcode,
        strings.productFormAddCategoryTitle,
        strings.productFormCategoryName,
        strings.productFormAddCategory,
        strings.productFormUnitLabel,
        strings.productFormUnitHint,
        strings.productFormTrackStockHint,
        strings.productFormActiveLabel,
        strings.productFormActiveHint,
        strings.productFormMoneyHint,
        strings.productFormQuantityHint,
        strings.productFormRequiredField(strings.productName),
        strings.productFormInvalidNumber(strings.mrpPrice),
        strings.productFormInvalidField(strings.unit),
        strings.productFormNegativeNumber(strings.currentStock),
        strings.productFormNonPositiveMrp,
        strings.productFormFractionNotAllowed(strings.unit),
        strings.productFormCategoryRequired,
        strings.productFormBarcodeInvalid,
        strings.productFormBarcodeSet("AB-1"),
        strings.productFormChooseCategory,
        strings.productFormFastAddPrefix,
        strings.productFormAddItem,
        strings.productFormRecentlyAdded,
        strings.productFormTotal,
        strings.productFormNoItemsInCategory,
        strings.productFormPrice,
        strings.productFormStock,
        strings.productFormUntracked,
        strings.productFormNoProducts,
        strings.productFormInactive,
        strings.productFormLowStock("2 pcs"),
        strings.productFormStockAvailable("8 pcs"),
        strings.productFormStockUntracked,
        strings.productFormAdjustStock,
        strings.productFormSaveAndAdd,
        strings.productFormCategoryManager,
        strings.productFormNewCategoryPlaceholder,
        strings.productFormRenameCategory,
        strings.productFormUpdateCategory,
        strings.productFormDone,
        strings.productFormNoLowStock,
        strings.productFormStockHealthy,
        strings.productFormAddStock,
        strings.productFormLeft("2 pcs"),
        strings.productFormClose,
        strings.productFormPlusOneStock,
        strings.productFormCopy,
        strings.productFormBarcodeScanned("AB-1"),
        strings.productFormLowStockBanner(2),
        strings.productFormReorderListHint,
        strings.productFormOrderList,
        strings.stockAdjustmentCurrentStock("2 kg"),
        strings.stockAdjustmentEnterPhysicalCount,
        strings.stockAdjustmentReason,
        strings.stockAdjustmentSave,
        strings.stockAdjustmentHistory,
        strings.stockAdjustmentStockTransition("3", "2"),
        strings.stockAdjustmentProductNotFound,
        strings.stockReasonCountCorrection,
        strings.stockReasonPurchaseAdded,
        strings.stockReasonDamaged,
        strings.stockReasonOpening,
        strings.stockReasonOther
    )
}
