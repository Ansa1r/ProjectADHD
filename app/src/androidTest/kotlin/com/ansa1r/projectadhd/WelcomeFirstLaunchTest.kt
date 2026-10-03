package com.ansa1r.projectadhd

import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ansa1r.projectadhd.domain.onboarding.OnboardingState
import com.ansa1r.projectadhd.domain.onboarding.OnboardingStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith

/** Run this class on a fresh test installation; never reset a user's onboarding data. */
@RunWith(AndroidJUnit4::class)
class WelcomeFirstLaunchTest {
    @get:Rule(order = 0) val firstLaunch = object : ExternalResource() {
        override fun before() {
            val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ProjectADHDApplication
            val state = runBlocking { app.container.preferences.onboarding.first() }
            assumeTrue("This first-launch test requires fresh onboarding data on a test device.", state == OnboardingState())
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun actualActivityPlacesEveryCoachAboveNavigationAndContinuesToHome() {
        val welcome = "Привет! Я Боб. Я помогу тебе следить за привычками и меньше отвлекаться."
        val home = "Это главная страница. Здесь видно твой прогресс за сегодня."
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("startup_cover").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText(welcome, useUnmergedTree = true).fetchSemanticsNodes().size == 1
        }
        val panel = compose.onNodeWithTag("bob_coach_panel").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("main_content_area", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val navigation = compose.onNodeWithTag("bottom_navigation", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val bob = compose.onNodeWithTag("bob_coach_mascot", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithTag("bob_coach_text", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(panel.bottom <= navigation.top)
        assertTrue(content.bottom <= panel.top)
        assertTrue(text.right <= bob.left)
        compose.onNodeWithTag("bottom_navigation", useUnmergedTree = true).assertIsDisplayed()
        val prefs = (compose.activity.application as ProjectADHDApplication).container.preferences
        val readyBeforeTap = isReady()
        compose.onNodeWithTag("bob_coach_primary", useUnmergedTree = true).performClick()
        if (!readyBeforeTap) {
            compose.waitUntil(10_000) { isReady() }
            assertEquals(OnboardingStep.WELCOME, runBlocking { prefs.onboarding.first().onboardingStep })
            assertEquals(panel, compose.onNodeWithTag("bob_coach_panel").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithTag("bob_coach_primary", useUnmergedTree = true).performClick()
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(home, useUnmergedTree = true).fetchSemanticsNodes().size == 1 }
        assertEquals(OnboardingStep.HOME, runBlocking { prefs.onboarding.first().onboardingStep })
        val nextPanel = compose.onNodeWithTag("bob_coach_panel").fetchSemanticsNode().boundsInRoot
        val nextNavigation = compose.onNodeWithTag("bottom_navigation", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(nextPanel.bottom <= nextNavigation.top)
    }

    private fun isReady(): Boolean = compose.onNodeWithTag("bob_coach_panel").fetchSemanticsNode()
        .config[SemanticsProperties.StateDescription] == "Готов к продолжению"
}
