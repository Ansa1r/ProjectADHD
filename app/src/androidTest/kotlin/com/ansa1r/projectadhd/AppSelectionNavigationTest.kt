package com.ansa1r.projectadhd

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ansa1r.projectadhd.domain.model.TrackedApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSelectionNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val container get() = (compose.activity.application as ProjectADHDApplication).container
    private fun ready() {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("startup_cover").fetchSemanticsNodes().isEmpty() }
    }
    private fun scrollTo(tag: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
    }
    private fun homeReached() {
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Настройки").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun withSelection(apps: List<TrackedApp>, block: () -> Unit) {
        ready()
        val original = runBlocking { container.trackedApps.observeAll().first() }
        try {
            runBlocking { container.saveTrackedSelection(apps) }
            block()
        } finally { runBlocking {
            container.saveTrackedSelection(original.filter { it.enabled })
            original.filterNot { it.enabled }.forEach { container.trackedApps.save(it) }
        } }
    }
    @Test fun homeIsReachableWithNoSelectedApps() = withSelection(emptyList()) {
        compose.onNodeWithContentDescription("Приложения").performClick()
        compose.onNodeWithTag("app_selection").assertIsDisplayed()
        compose.onNodeWithContentDescription("Главная").performClick()
        homeReached()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Выбрать приложения"))
        compose.onNodeWithText("Выбрать приложения").assertIsDisplayed()
        assertTrue(runBlocking { container.trackedApps.observeAll().first().isEmpty() })
    }
    @Test fun leavingWithoutSaveDiscardsDraftAndNeverChangesDatabase() {
        val old = TrackedApp("test.draft", "Draft test", 20)
        withSelection(listOf(old)) {
            compose.onNodeWithContentDescription("Приложения").performClick()
            scrollTo("app_choice_test.draft")
            compose.onNodeWithTag("app_choice_test.draft").assertIsOn().performClick()
            compose.onNodeWithTag("selection_action").assertIsDisplayed()
            compose.onNodeWithContentDescription("Главная").performClick()
            homeReached()
            assertEquals(listOf(old), runBlocking { container.trackedApps.observeAll().first() })
            compose.onNodeWithContentDescription("Приложения").performClick()
            scrollTo("app_choice_test.draft")
            compose.onNodeWithTag("app_choice_test.draft").assertIsOn()
            compose.onNodeWithTag("startup_cover").assertDoesNotExist()
        }
    }
    @Test fun limitSetupAppliesCommonThenIndividualLimitAndSavesToHome() {
        val old = listOf(TrackedApp("test.first", "First test", 10), TrackedApp("test.second", "Second test", 20))
        withSelection(old) {
            compose.onNodeWithContentDescription("Приложения").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("edit_limits").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("edit_limits").performClick()
            scrollTo("common_limit")
            compose.onNodeWithTag("common_limit").performTextReplacement("25")
            compose.onNodeWithText("Применить ко всем").performScrollTo().performClick()
            scrollTo("limit_test.first")
            compose.onNodeWithTag("limit_test.first").performTextReplacement("40")
            assertEquals(old.toSet(), runBlocking { container.trackedApps.observeAll().first().toSet() })
            compose.onNodeWithTag("save_selection").performClick()
            homeReached()
            assertEquals(mapOf("test.first" to 40, "test.second" to 25),
                runBlocking { container.trackedApps.observeAll().first().associate { it.packageName to it.sessionLimitMinutes } })
            compose.onNode(hasScrollAction()).performScrollToIndex(3)
            compose.onNodeWithText("Выбрать приложения").assertDoesNotExist()
        }
    }
    @Test fun removeAllSavesEmptySelectionAndRestoresHomeCta() {
        withSelection(listOf(TrackedApp("test.first", "First test"), TrackedApp("test.second", "Second test"))) {
            compose.onNodeWithContentDescription("Приложения").performClick()
            for (name in listOf("test.first", "test.second")) {
                scrollTo("app_choice_" + name)
                compose.onNodeWithTag("app_choice_" + name).performClick()
            }
            compose.onNodeWithTag("selection_action").performClick()
            homeReached()
            assertTrue(runBlocking { container.trackedApps.observeAll().first().isEmpty() })
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Выбрать приложения"))
            compose.onNodeWithText("Выбрать приложения").assertIsDisplayed()
        }
    }
    @Test fun profileActionsOpenRealScreensAndInternalNavigationDoesNotReplayStartup() {
        ready()
        compose.onNodeWithTag("home_profile").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Мой прогресс"))
        compose.onNodeWithText("Мой прогресс").performClick()
        compose.onNodeWithTag("startup_cover").assertDoesNotExist()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Редактировать профиль"))
        compose.onNodeWithText("Редактировать профиль").performClick()
        compose.onNodeWithTag("nickname_input").assertIsDisplayed()
        compose.onNodeWithTag("change_avatar").assertExists()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithContentDescription("Назад").performClick()
        homeReached()
    }
}
