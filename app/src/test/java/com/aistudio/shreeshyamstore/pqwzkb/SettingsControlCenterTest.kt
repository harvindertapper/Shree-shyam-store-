package com.aistudio.shreeshyamstore.pqwzkb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppOutlinedTextField
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.SettingsInfoCard
import com.aistudio.shreeshyamstore.pqwzkb.ui.screens.SettingsSectionHeading
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsControlCenterTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun groupedSettingsSummaryIsVisibleAndActionable() {
        composeTestRule.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.testTag("settings_test_content")
                ) {
                    SettingsSectionHeading("Stock & products")
                    SettingsInfoCard(
                        title = "Stock & products",
                        detail = "Global defaults are not available yet.",
                        testTag = "settings_inventory_summary_card"
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("settings_inventory_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Global defaults are not available yet.").assertIsDisplayed()
    }

    @Test
    fun settingsSummaryTagCanBeLocatedWithoutScreenshotRendering() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsInfoCard(
                    title = "Data & privacy",
                    detail = "Recovery point is created before restore.",
                    testTag = "settings_data_privacy_card"
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_data_privacy_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data & privacy").assertIsDisplayed()
    }

    @Test
    fun settingsFormAcceptsKeyboardInput() {
        composeTestRule.setContent {
            MyApplicationTheme {
                val ownerName = remember { mutableStateOf("") }
                AppOutlinedTextField(
                    value = ownerName.value,
                    onValueChange = { ownerName.value = it },
                    label = "Owner name",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.testTag("settings_owner_name_test_field")
                )
            }
        }

        val ownerNameField = composeTestRule.onNodeWithTag("settings_owner_name_test_field")
        ownerNameField.performTextInput("Owner")
        ownerNameField.assertTextContains("Owner")
    }

    @Test
    fun settingsControlCenterCanScrollToTheBottomSummary() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .testTag("settings_scroll_container")
                ) {
                    repeat(12) { index ->
                        Text("Settings section $index")
                    }
                    SettingsInfoCard(
                        title = "Support & about",
                        detail = "Version and support guidance.",
                        testTag = "settings_bottom_summary"
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("settings_bottom_summary")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
