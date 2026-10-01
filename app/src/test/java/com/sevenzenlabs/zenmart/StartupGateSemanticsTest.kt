package com.sevenzenlabs.zenmart

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.sevenzenlabs.zenmart.ui.screens.StartupScreen
import com.sevenzenlabs.zenmart.ui.theme.MyApplicationTheme
import com.sevenzenlabs.zenmart.utils.SyncManager
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StartupGateSemanticsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun startupScreenDisplaysStoreIconAndProgressIndicatorInLightTheme() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                StartupScreen()
            }
        }

        composeTestRule.onNodeWithTag("startup_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("startup_store_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("startup_progress").assertIsDisplayed()
    }

    @Test
    fun startupScreenDisplaysStoreIconAndProgressIndicatorInDarkTheme() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                StartupScreen()
            }
        }

        composeTestRule.onNodeWithTag("startup_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("startup_store_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("startup_progress").assertIsDisplayed()
    }

    @Test
    fun disablingAutomaticSyncSafelyCleansUpNetworkCallbackLifecycle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SyncManager.resetForTesting()

        // Disable automatic sync -> callback must be cleanly unregistered / remain unregistered
        SyncManager.configureAutomaticSync(context, enabled = false)
        assertFalse(SyncManager.isNetworkCallbackRegisteredForTesting())

        // Multiple disablings must be idempotent and never throw or leak
        SyncManager.configureAutomaticSync(context, enabled = false)
        assertFalse(SyncManager.isNetworkCallbackRegisteredForTesting())

        SyncManager.resetForTesting()
    }
}
