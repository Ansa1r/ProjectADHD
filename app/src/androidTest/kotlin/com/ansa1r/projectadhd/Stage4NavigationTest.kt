package com.ansa1r.projectadhd

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Stage4NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun ready() { compose.waitUntil(5_000) { compose.onAllNodesWithTag("startup_cover").fetchSemanticsNodes().isEmpty() } }
    @Test fun mascotHasSeparateRouteAndCustomizationOpensWithoutStartupReplay() {
        ready()
        compose.onNodeWithContentDescription("Маскот").performClick()
        compose.onNodeWithTag("mascot_name").assertIsDisplayed()
        compose.onNodeWithTag("profile_nickname").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Кастомизация"))
        compose.onNodeWithText("Кастомизация").performClick()
        compose.onNodeWithText("Функция появится в следующих версиях ProjectADHD.").assertIsDisplayed()
        compose.onNodeWithTag("startup_cover").assertDoesNotExist()
    }
    @Test fun creationIsFullScreenAndBackDiscardsUnsavedHabit() {
        ready()
        compose.onNodeWithContentDescription("Привычки").performClick()
        compose.onNodeWithTag("add_habit").performClick()
        compose.onNodeWithTag("habit_title").performTextInput("Unsaved test")
        compose.onNodeWithText("Время в день · HH:MM").assertIsDisplayed()
        compose.onNodeWithText("Без приложения").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Unsaved test").assertDoesNotExist()
    }
}
