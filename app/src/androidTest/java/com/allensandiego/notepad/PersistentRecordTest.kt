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
class PersistentRecordTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("supabase_prefs_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    @Test
    fun createRecordAndLeaveIt() {
        // 1. App starts directly in MainScreen (offline by default)
        composeTestRule.waitForIdle()

        // 2. Create Collection
        if (composeTestRule.onAllNodesWithContentDescription("Menu").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithContentDescription("Menu").performClick()
        }
        composeTestRule.onNodeWithContentDescription("Add Collection").performClick()
        val tableName = "Persistent Test Table"
        composeTestRule.onNodeWithText("Collection Name").performTextInput(tableName)
        composeTestRule.onNodeWithText("Create").performClick()
        
        // 3. Add Field (Gear icon next to collection -> Add Field dialog -> Close)
        composeTestRule.onNodeWithText(tableName).assertIsDisplayed()
        composeTestRule.onNode(
            hasContentDescription("Configure Schema") and hasAnyAncestor(hasText(tableName))
        ).performClick()
        composeTestRule.onNodeWithContentDescription("Add Field").performClick()
        composeTestRule.onNodeWithText("Field Name").performTextInput("Note")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        // 4. Select collection in sidebar and create Record
        composeTestRule.onNodeWithText(tableName).performClick()
        composeTestRule.onNodeWithContentDescription("Add Record").performClick()
        composeTestRule.onNode(
            hasSetTextAction() and hasParent(hasAnyDescendant(hasText("Note")))
        ).performTextInput("This record was created by the automated test and NOT deleted.")
        composeTestRule.onNodeWithContentDescription("Save").performClick()
        
        // Verify it is there
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithContentDescription("Delete Record").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("This record was created by the automated test and NOT deleted.").assertIsDisplayed()
        
        // We finish here without deleting.
    }
}
