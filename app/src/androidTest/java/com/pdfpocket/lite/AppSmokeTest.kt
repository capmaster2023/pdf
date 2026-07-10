package com.pdfpocket.lite

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test fun applicationOpensOnHome() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.app_name)).assertIsDisplayed()
    }

    @Test fun mainNavigationWorks() {
        val documents = composeRule.activity.getString(R.string.documents)
        val settings = composeRule.activity.getString(R.string.settings)
        composeRule.onNodeWithText(documents).performClick()
        composeRule.onNodeWithText(documents).assertIsDisplayed()
        composeRule.onNodeWithText(settings).performClick()
        composeRule.onNodeWithText(settings).assertIsDisplayed()
    }

    @Test fun openPdfButtonIsAvailable() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.open_pdf)).assertIsDisplayed()
    }
}
