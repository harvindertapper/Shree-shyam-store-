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
import androidx.compose.ui.text.input.KeyboardOptions
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Issue56JourneySemanticsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hindiCriticalJourneysRemainReadableAndReachable() {
        assertCriticalJourneySurfaces(AppLanguage.HINDI)
    }

    @Test
    fun englishCriticalJourneysRemainReadableAndReachable() {
        assertCriticalJourneySurfaces(AppLanguage.ENGLISH)
    }

    private fun assertCriticalJourneySurfaces(language: AppLanguage) {
        val strings = LocaleHelper.getStrings(language)
        val retryStatus = MutationStatus(
            stage = MutationStage.RETRYABLE_ERROR,
            message = strings.statusRetryableError,
            canRetry = true
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                val shopName = remember { mutableStateOf("") }
                val productName = remember { mutableStateOf("") }
                val customerName = remember { mutableStateOf("") }
                val receivedAmount = remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .testTag("issue56_journey_scroll")
                ) {
                    Text(strings.shopSetupTitle, modifier = Modifier.testTag("journey_onboarding_title"))
                    AppOutlinedTextField(
                        value = shopName.value,
                        onValueChange = { shopName.value = it },
                        label = strings.shopNameLabel,
                        supportingText = { Text(strings.setupShopNameHint) },
                        modifier = Modifier.testTag("shop_name_input")
                    )
                    AppOutlinedTextField(
                        value = productName.value,
                        onValueChange = { productName.value = it },
                        label = strings.productName,
                        modifier = Modifier.testTag("product_name_input")
                    )
                    AppDropdownMenuSurface(
                        expanded = true,
                        onDismissRequest = {},
                        modifier = Modifier.testTag("journey_category_menu")
                    ) {
                        AppDropdownMenuItem(
                            text = strings.addNewCategory,
                            onClick = {},
                            modifier = Modifier.testTag("journey_add_category_item")
                        )
                    }
                    AppOutlinedTextField(
                        value = customerName.value,
                        onValueChange = { customerName.value = it },
                        label = strings.customerName,
                        modifier = Modifier.testTag("customer_search_input")
                    )
                    AppOutlinedTextField(
                        value = receivedAmount.value,
                        onValueChange = { receivedAmount.value = it },
                        label = strings.billingReceivedCashUpi,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.testTag("received_amount_field")
                    )
                    AppPrimaryButton(
                        text = strings.saveProduct,
                        onClick = {},
                        modifier = Modifier.testTag("save_product_button")
                    )
                    AppMutationStatusCard(
                        status = retryStatus,
                        strings = strings,
                        onRetry = {},
                        onDismiss = {},
                        modifier = Modifier.testTag("journey_mutation_status")
                    )
                    ReportIntervalTabs(
                        strings = strings,
                        selectedInterval = ReportInterval.CUSTOM,
                        onSelected = {},
                        modifier = Modifier.testTag("journey_report_intervals")
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
                        modifier = Modifier.testTag("journey_report_custom_range")
                    )
                    ReportLoadingState(
                        strings = strings,
                        modifier = Modifier.testTag("journey_report_loading")
                    )
                    ReportErrorState(
                        strings = strings,
                        onRetry = {},
                        modifier = Modifier.testTag("journey_report_error")
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("journey_onboarding_title")
            .assertIsDisplayed()
            .assertTextContains(strings.shopSetupTitle)
        composeTestRule.onNodeWithTag("shop_name_input")
            .performTextInput("Shree Shyam Store")
            .assertTextContains("Shree Shyam Store")
        composeTestRule.onNodeWithTag("product_name_input")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("journey_add_category_item").assertIsDisplayed()
        composeTestRule.onNodeWithTag("customer_search_input")
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("Customer")
            .assertTextContains("Customer")
        composeTestRule.onNodeWithTag("received_amount_field")
            .performScrollTo()
            .assertIsDisplayed()
            .performTextInput("125.00")
            .assertTextContains("125.00")
        composeTestRule.onNodeWithTag("save_product_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(strings.saveProduct)
        composeTestRule.onNodeWithTag("mutation_status_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("mutation_retry_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("journey_report_intervals").assertIsDisplayed()
        composeTestRule.onNodeWithTag("report_select_start_date")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(strings.reportsStartDate)
        composeTestRule.onNodeWithTag("journey_report_loading")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(strings.reportsLoading).assertIsDisplayed()
        composeTestRule.onNodeWithTag("report_retry_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(strings.reportsRetry)
    }
}
