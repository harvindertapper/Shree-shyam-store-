package com.aistudio.shreeshyamstore.pqwzkb

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppDropdownMenuItem
import com.aistudio.shreeshyamstore.pqwzkb.ui.components.AppDropdownMenuSurface
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDropdownMenuTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sharedMenuExposesReadableSelectedOption() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AppDropdownMenuSurface(
                    expanded = true,
                    onDismissRequest = {},
                    modifier = Modifier.testTag("shared_menu")
                ) {
                    AppDropdownMenuItem(
                        text = "Selected Category",
                        onClick = {},
                        modifier = Modifier.testTag("selected_menu_item")
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("selected_menu_item").performClick()
        composeTestRule.onNodeWithText("Selected Category").performClick()
    }
}
