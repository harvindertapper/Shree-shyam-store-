package com.aistudio.shreeshyamstore.pqwzkb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportDate
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportDateRange
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportInterval
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportRangeResult
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.CustomReportRangeSelector
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.ReportEmptyState
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.ReportErrorState
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.ReportIntervalTabs
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.ReportLoadingState
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.MyApplicationTheme
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Issue55ReportSemanticsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hindiReportStatesRemainReadableAndReachable() {
        assertReportStates(LocaleHelper.getStrings(AppLanguage.HINDI))
    }

    @Test
    fun englishReportStatesRemainReadableAndReachable() {
        assertReportStates(LocaleHelper.getStrings(AppLanguage.ENGLISH))
    }

    private fun assertReportStates(strings: AppStrings) {
        composeTestRule.setContent {
            MyApplicationTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .testTag("issue55_report_scroll")
                ) {
                    ReportIntervalTabs(
                        strings = strings,
                        selectedInterval = ReportInterval.CUSTOM,
                        onSelected = {},
                        modifier = Modifier.testTag("issue55_report_intervals")
                    )
                    Text(strings.today)
                    Text(strings.thisWeek)
                    Text(strings.thisMonth)
                    Text(strings.allTime)
                    Text(strings.customRange)
                    CustomReportRangeSelector(
                        strings = strings,
                        startDate = ReportDate(2026, 8, 1),
                        endDate = ReportDate(2026, 8, 7),
                        rangeResult = ReportRangeResult.Valid(
                            ReportDateRange(startInclusiveMillis = 1L, endExclusiveMillis = 2L)
                        ),
                        onSelectStart = {},
                        onSelectEnd = {},
                        onClear = {},
                        modifier = Modifier.testTag("issue55_custom_range")
                    )
                    ReportLoadingState(
                        strings = strings,
                        modifier = Modifier.testTag("issue55_loading")
                    )
                    ReportEmptyState(
                        title = strings.reportsNoSalesInRange,
                        detail = strings.reportsNoRecords,
                        modifier = Modifier.testTag("issue55_empty")
                    )
                    ReportErrorState(
                        strings = strings,
                        onRetry = {},
                        modifier = Modifier.testTag("issue55_error")
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("issue55_report_intervals").assertIsDisplayed()
        composeTestRule.onNodeWithText(strings.thisWeek).assertTextContains(strings.thisWeek)
        composeTestRule.onNodeWithText(strings.customRange).assertTextContains(strings.customRange)
        composeTestRule.onNodeWithTag("report_select_start_date")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(strings.reportsStartDate)
        composeTestRule.onNodeWithTag("report_select_end_date")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(strings.reportsEndDate)
        composeTestRule.onNodeWithTag("issue55_loading")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(strings.reportsLoading).assertIsDisplayed()
        composeTestRule.onNodeWithTag("report_retry_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(strings.reportsRetry).assertIsDisplayed()
    }
}
