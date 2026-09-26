package com.sevenzenlabs.zenmart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sevenzenlabs.zenmart.utils.AppLanguage
import com.sevenzenlabs.zenmart.utils.LocaleHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Issue56MainActivitySmokeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun signedOutStartupExposesLanguageSignInAndLocalContinuation() {
        composeTestRule.onNodeWithTag("language_switcher_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("google_sign_in_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("skip_login_button")
            .performScrollTo()
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("lang_en_button")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("google_sign_in_button")
            .performScrollTo()
            .assertIsDisplayed()
        val englishGoogleSignInLabel =
            LocaleHelper.getStrings(AppLanguage.ENGLISH).continueWithGoogle
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(englishGoogleSignInLabel)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("google_sign_in_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals(englishGoogleSignInLabel)
        composeTestRule.onNodeWithTag("skip_login_button")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
