package com.aistudio.shreeshyamstore.pqwzkb

import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import org.junit.Assert.assertFalse
import org.junit.Test

class Issue55ReportLocaleContractTest {
    @Test
    fun reportStringsRenderInBothLanguages() {
        listOf(AppLanguage.HINDI, AppLanguage.ENGLISH).forEach { language ->
            val strings = LocaleHelper.getStrings(language)
            reportStrings(strings).forEachIndexed { index, value ->
                assertFalse(
                    "Issue #55 locale value at index $index must not be blank for $language",
                    value.isBlank()
                )
            }
        }
    }

    private fun reportStrings(strings: AppStrings): List<String> = listOf(
        strings.reportsRangeSummary("01/08/2026", "07/08/2026"),
        strings.reportsStartDate,
        strings.reportsEndDate,
        strings.reportsSelectStartDate,
        strings.reportsSelectEndDate,
        strings.reportsApplyCustomRange,
        strings.reportsClearCustomRange,
        strings.reportsInvalidCustomRange,
        strings.reportsLoading,
        strings.reportsLoadError,
        strings.reportsRetry,
        strings.reportsNoSalesInRange,
        strings.reportsExportReady,
        strings.reportsExportEmpty,
        strings.reportsExportFailed,
        strings.reportsPaymentState,
        strings.reportsPaymentNotRequired,
        strings.reportsPaymentPending,
        strings.reportsPaymentReceived,
        strings.reportsPaymentFailed,
        strings.reportsPaymentPartiallyRefunded,
        strings.reportsPaymentRefunded,
        strings.reportsPaymentUnknown
    )
}
