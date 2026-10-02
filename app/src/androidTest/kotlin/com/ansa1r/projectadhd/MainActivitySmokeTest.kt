package com.ansa1r.projectadhd

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule(order = 0) val onboarding = CompletedOnboardingRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Test fun launchesAndNavigatesToHabitCreation() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("startup_cover").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("home_profile").assertIsDisplayed()
        compose.onNodeWithContentDescription("Привычки").performClick()
        compose.onNodeWithText("Добавить привычку").assertIsDisplayed().performClick()
        compose.onNodeWithText("Название привычки").assertIsDisplayed()
    }
}
