package com.aistudio.shreeshyamstore.pqwzkb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportDate
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportDateRange
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportInterval
import com.aistudio.shreeshyamstore.pqwzkb.commerce.ReportRangeResult
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppDropdownMenuItem
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppDropdownMenuSurface
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppMutationStatusCard
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppOutlinedTextField
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppPrimaryButton
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.CustomReportRangeSelector
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.ReportErrorState
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.ReportIntervalTabs
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.ReportLoadingState
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.MyApplicationTheme
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppLanguage
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings
import com.aistudio.shreeshyamstore.pqwzkb.utils.LocaleHelper
import com.aistudio.shreeshyamstore.pqwzkb.utils.MutationStage
import com.aistudio.shreeshyamstore.pqwzkb.utils.MutationStatus
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Issue56DeviceJourneyTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun englishCriticalJourneySurfacesFitOnAndroid() {
        assertCriticalJourneySurfaces(LocaleHelper.getStrings(AppLanguage.ENGLISH))
    }

    @Test
    fun hindiCriticalJourneySurfacesFitOnAndroid() {
        assertCriticalJourneySurfaces(LocaleHelper.getStrings(AppLanguage.HINDI))
    }

    private fun assertCriticalJourneySurfaces(strings: AppStrings) {
        composeTestRule.setContent {
            MyApplicationTheme {
                val shopName = remember { mutableStateOf("") }
                val customerName = remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .testTag("device_journey_scroll")
                ) {
                    Text(strings.shopSetupTitle, modifier = Modifier.testTag("device_onboarding_title"))
                    AppOutlinedTextField(
                        value = shopName.value,
                        onValueChange = { shopName.value = it },
                        label = strings.shopNameLabel,
                        supportingText = { Text(strings.setupShopNameHint) },
                        modifier = Modifier.testTag("device_shop_name_input")
                    )
                    AppDropdownMenuSurface(
                        expanded = true,
                        onDismissRequest = {},
                        modifier = Modifier.testTag("device_category_menu")
                    ) {
                        AppDropdownMenuItem(
                            text = strings.addNewCategory,
                            onClick = {},
                            modifier = Modifier.testTag("device_add_category_item")
                        )
                    }
                    AppOutlinedTextField(
                        value = customerName.value,
                        onValueChange = { customerName.value = it },
                        label = strings.searchCustomer,
                        modifier = Modifier.testTag("device_customer_search")
                    )
                    AppPrimaryButton(
                        text = strings.saveProduct,
                        onClick = {},
                        modifier = Modifier.testTag("device_save_product")
                    )
                    AppMutationStatusCard(
                        status = MutationStatus(
                            stage = MutationStage.RETRYABLE_ERROR,
                            message = strings.statusRetryableError,
                            canRetry = true
                        ),
                        strings = strings,
                        onRetry = {},
                        modifier = Modifier.testTag("device_mutation_status")
                    )
                    ReportIntervalTabs(
                        strings = strings,
                        selectedInterval = ReportInterval.CUSTOM,
                        onSelected = {},
                        modifier = Modifier.testTag("device_report_intervals")
                    )
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
                        modifier = Modifier.testTag("device_custom_range")
                    )
                    ReportLoadingState(
                        strings = strings,
                        modifier = Modifier.testTag("device_report_loading")
                    )
                    ReportErrorState(
                        strings = strings,
                        onRetry = {},
                        modifier = Modifier.testTag("device_report_error")
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("device_onboarding_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("device_shop_name_input")
            .performTextInput("Shree Shyam Store")
        composeTestRule.onNodeWithTag("device_shop_name_input")
            .assertTextContains("Shree Shyam Store")
        composeTestRule.onNodeWithTag("device_add_category_item").assertIsDisplayed()
        composeTestRule.onNodeWithTag("device_customer_search")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("device_customer_search")
            .performTextInput("Customer")
        composeTestRule.onNodeWithTag("device_customer_search")
            .assertTextContains("Customer")
        composeTestRule.onNodeWithTag("device_save_product")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(strings.saveProduct)
        composeTestRule.onNodeWithText(strings.statusRetryableError)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("mutation_retry_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("device_report_intervals").assertIsDisplayed()
        composeTestRule.onNodeWithTag("report_select_start_date")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(strings.reportsStartDate)
        composeTestRule.onNodeWithTag("device_report_loading")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("report_retry_button")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
