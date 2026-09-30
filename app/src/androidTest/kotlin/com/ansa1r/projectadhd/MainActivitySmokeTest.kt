package com.ansa1r.projectadhd

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun launchesAndNavigatesToHabitCreation() {
        compose.onNodeWithText("ProjectADHD").assertIsDisplayed()
        compose.onNodeWithText("Привычки").performClick()
        compose.onNodeWithText("Добавить привычку").assertIsDisplayed().performClick()
        compose.onNodeWithText("Название привычки").assertIsDisplayed()
    }
}
