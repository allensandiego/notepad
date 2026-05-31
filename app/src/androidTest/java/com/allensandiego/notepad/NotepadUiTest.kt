package com.allensandiego.notepad

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotepadUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppLaunchAndShowWelcomeScreen() {
        // Since we are running on a clean install, the app should open the MainScreen in offline mode
        // displaying the welcome message.
        composeTestRule.onNodeWithText("Welcome to Notepad", ignoreCase = true).assertIsDisplayed()
    }
}
