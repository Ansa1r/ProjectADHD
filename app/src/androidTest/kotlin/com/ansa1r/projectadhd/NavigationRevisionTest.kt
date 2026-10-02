package com.ansa1r.projectadhd

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationRevisionTest {
    @get:Rule(order = 0) val onboarding = CompletedOnboardingRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private fun ready() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("startup_cover").fetchSemanticsNodes().isEmpty() }
    }
    @Test fun bottomNavigationHasFiveUnlabelledAccessibleIconsAndProfile() {
        ready()
        compose.onAllNodes(hasAnyAncestor(hasTestTag("bottom_navigation")) and hasClickAction()).assertCountEquals(5)
        for (name in listOf("Главная", "Привычки", "Приложения", "Статистика", "Маскот")) {
            compose.onNodeWithContentDescription(name).assertIsDisplayed()
            compose.onAllNodes(hasText(name) and hasAnyAncestor(hasTestTag("bottom_navigation"))).assertCountEquals(0)
        }
        compose.onNodeWithTag("home_profile").performClick()
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
        compose.onNodeWithText("Интервалы уведомлений").performClick()
        compose.onNodeWithTag("opacity_slider").assertDoesNotExist()
        compose.onNodeWithTag("opacity_preview").assertDoesNotExist()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Для разработчика").performScrollTo().performClick()
        compose.onNodeWithText("Отладка").performClick()
        compose.onNodeWithText("Инструменты разработчика доступны только в debug-сборке.").assertExists()
    }
}
