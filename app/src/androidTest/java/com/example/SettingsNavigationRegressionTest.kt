package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.SettingsDataStore
import com.example.data.dataStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsNavigationRegressionTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private companion object {
        const val SEEDED_SHOP_NAME = "QA Seeded Store"
    }

    @Before
    fun seedLoggedInSession() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.dataStore.edit { it.clear() }
            SettingsDataStore(context).apply {
                updateShopName(SEEDED_SHOP_NAME)
                saveSession(username = "qa_owner", email = "qa@example.com")
                setFirstLaunchCompleted(true)
                updateSelectedLanguage("en")
            }
        }
    }

    @Test
    fun settingsScreenOpensWithLanguageSelector() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText(SEEDED_SHOP_NAME).fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag("skip_button").assertIsDisplayed().performClick()

            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithTag("nav_settings").fetchSemanticsNodes().isNotEmpty()
            }

            composeRule.onNodeWithTag("nav_settings").performClick()

            composeRule.onNodeWithText("App language").assertIsDisplayed()
            composeRule.onNodeWithText("English").assertIsDisplayed()
            composeRule.onNodeWithText("Hindi").assertIsDisplayed()
        }
    }
}
