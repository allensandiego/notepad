package com.allensandiego.notepad

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullAppFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val testUrl = "https://kbabpyjwqxsipgtemtne.supabase.co"
    private val testApiKey = "sb_publishable_DQNjEdsL0iNFatdoZa0YnQ_A6cTjsQI"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("supabase_prefs_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun testFullApplicationFlowWithSupabase() {
        // Wait for app to be idle
        composeTestRule.waitForIdle()

        // 1. Connection setup: Navigate to settings -> Connection screen
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Set Up Sync Connection", ignoreCase = true).performClick()

        // 2. Connection Screen: Enter real credentials (NO PASSWORD)
        composeTestRule.onNodeWithText("Supabase Project URL").performTextReplacement(testUrl)
        composeTestRule.onNodeWithText("Supabase Anon API Key").performTextReplacement(testApiKey)
        
        // Ensure keyboard is gone so button is clickable
        composeTestRule.onNodeWithText("Connect Database").performClick()

        // Wait for connection to succeed and dialog to close
        composeTestRule.waitForIdle()

        // Go back from Settings to MainScreen
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        // 3. Create a new collection
        if (composeTestRule.onAllNodesWithContentDescription("Menu").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithContentDescription("Menu").performClick()
        }
        composeTestRule.onNodeWithContentDescription("Add Collection").performClick()
        
        val tableName = "Cloud Table ${System.currentTimeMillis()}"
        composeTestRule.onNodeWithText("Collection Name").performTextInput(tableName)
        composeTestRule.onNodeWithText("Create").performClick()
        
        // 4. Manage Fields: Add a column (field)
        composeTestRule.onNodeWithText(tableName).assertIsDisplayed()
        composeTestRule.onNode(
            hasContentDescription("Configure Schema") and hasAnyAncestor(hasText(tableName))
        ).performClick()
        
        composeTestRule.onNodeWithContentDescription("Add Field").performClick()
        composeTestRule.onNodeWithText("Field Name").performTextInput("Cloud Note")
        composeTestRule.onNodeWithText("Add").performClick()
        
        composeTestRule.onNodeWithText("Cloud Note").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        // 5. Select collection and add a record
        composeTestRule.onNodeWithText(tableName).performClick()
        composeTestRule.onNodeWithContentDescription("Add Record").performClick()
        
        // 6. Record Edit: Save record
        composeTestRule.onNode(
            hasSetTextAction() and hasParent(hasAnyDescendant(hasText("Cloud Note")))
        ).performTextInput("Synced via REST API!")
        composeTestRule.onNodeWithContentDescription("Save").performClick()
        
        composeTestRule.waitForIdle()
        
        // 7. Verify record in list
        composeTestRule.onNode(
            hasText("Synced via REST API!") and !hasSetTextAction()
        ).assertIsDisplayed()
        
        // 8. Cleanup record
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithContentDescription("Delete Record").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Delete Record").performClick()
        
        // 9. Cleanup collection
        if (composeTestRule.onAllNodesWithContentDescription("Menu").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithContentDescription("Menu").performClick()
        }
        composeTestRule.onNode(
            hasContentDescription("Delete Collection") and hasAnyAncestor(hasText(tableName))
        ).performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
    }
}
