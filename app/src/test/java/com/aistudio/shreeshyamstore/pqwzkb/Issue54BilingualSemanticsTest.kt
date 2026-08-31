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
class Issue54BilingualSemanticsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hindiCriticalMessagesRemainVisibleAndScrollable() {
        assertCriticalMessagesAreVisible(LocaleHelper.getStrings(AppLanguage.HINDI))
    }

    @Test
    fun englishCriticalMessagesRemainVisibleAndScrollable() {
        assertCriticalMessagesAreVisible(LocaleHelper.getStrings(AppLanguage.ENGLISH))
    }

    private fun assertCriticalMessagesAreVisible(strings: AppStrings) {
        val setupError = strings.setupPinInvalid
        val stockWarning = strings.productFormLowStock("2 kg")
        val paymentWarning = strings.billingInsufficientStockMessage("Rice", "2 kg")
        val ledgerHint = strings.udhaarNoteModePlaceholder

        composeTestRule.setContent {
            MyApplicationTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .testTag("issue54_bilingual_scroll")
                ) {
                    Text(strings.shopSetupTitle)
                    Text(setupError, modifier = Modifier.testTag("issue54_setup_error"))
                    Text(stockWarning, modifier = Modifier.testTag("issue54_stock_warning"))
                    Text(paymentWarning, modifier = Modifier.testTag("issue54_payment_warning"))
                    Text(ledgerHint, modifier = Modifier.testTag("issue54_ledger_hint"))
                    Text(strings.udhaarSavePayment, modifier = Modifier.testTag("issue54_primary_action"))
                }
            }
        }

        composeTestRule.onNodeWithTag("issue54_setup_error")
            .assertIsDisplayed()
            .assertTextContains(setupError)
        composeTestRule.onNodeWithText(stockWarning).assertIsDisplayed()
        composeTestRule.onNodeWithText(paymentWarning).assertIsDisplayed()
        composeTestRule.onNodeWithTag("issue54_ledger_hint")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains(ledgerHint)
        composeTestRule.onNodeWithText(strings.udhaarSavePayment).assertIsDisplayed()
    }
}
