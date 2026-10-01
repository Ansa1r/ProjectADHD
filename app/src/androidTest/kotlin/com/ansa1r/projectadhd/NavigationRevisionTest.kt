package com.ansa1r.projectadhd

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationRevisionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun ready() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("startup_cover").fetchSemanticsNodes().isEmpty() }
    }
    @Test fun bottomNavigationHasFiveUnlabelledAccessibleIconsAndProfile() {
        ready()
        compose.onAllNodes(hasAnyAncestor(hasTestTag("bottom_navigation")) and hasClickAction()).assertCountEquals(5)
        for (name in listOf("Главная", "Привычки", "Приложения", "Статистика", "Профиль")) {
            compose.onNodeWithContentDescription(name).assertIsDisplayed()
            compose.onAllNodes(hasText(name) and hasAnyAncestor(hasTestTag("bottom_navigation"))).assertCountEquals(0)
        }
        compose.onNodeWithContentDescription("Профиль").performClick()
        compose.onNodeWithTag("profile_nickname").assertIsDisplayed()
    }
    @Test fun settingsMenuOpensAllDestinationsAndDeveloperDebug() {
        ready()
        compose.onNodeWithText("Отладка").assertDoesNotExist()
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithTag("bottom_navigation").assertDoesNotExist()
        for ((menu, body) in listOf(
            "Разрешения" to "Нужен для определения времени использования выбранных приложений.",
            "Тема интерфейса" to "Настройки темы появятся в следующих версиях ProjectADHD.",
            "Конфиденциальность" to "Настройки конфиденциальности появятся в следующих версиях ProjectADHD."
        )) {
            compose.onNodeWithText(menu).performClick()
            compose.onNodeWithText(body).assertIsDisplayed()
            compose.onNodeWithContentDescription("Назад").performClick()
        }
        compose.onNodeWithText("Экран блокировки").performClick()
        compose.onNodeWithTag("opacity_slider").assertIsDisplayed()
        compose.onNodeWithTag("opacity_preview").assertExists()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Для разработчика").performScrollTo().performClick()
        compose.onNodeWithText("Отладка").performClick()
        compose.onNodeWithText("Инструменты разработчика доступны только в debug-сборке.").assertExists()
    }
}
