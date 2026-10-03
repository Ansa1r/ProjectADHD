package com.ansa1r.projectadhd

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

    @Test fun actualActivityShowsWelcomeAtSafeTopAndContinuesToHome() {
        val welcome = "Привет! Я Боб. Я помогу тебе следить за привычками и меньше отвлекаться."
        val home = "Это главная страница. Здесь видно твой прогресс за сегодня."
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("startup_cover").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithText(welcome).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText(welcome).assertIsDisplayed()
        val screen = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val safeArea = compose.onNodeWithTag("bob_coach_safe_area").fetchSemanticsNode().boundsInRoot
        val panel = compose.onNodeWithTag("bob_coach_panel").fetchSemanticsNode().boundsInRoot
        val navigation = compose.onNodeWithTag("bottom_navigation", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val bob = compose.onNodeWithTag("bob_coach_mascot").fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithText(welcome).fetchSemanticsNode().boundsInRoot
        val gapPx = 8f * compose.activity.resources.displayMetrics.density
        var safeTopPx = 0
        compose.runOnIdle {
            val insets = requireNotNull(ViewCompat.getRootWindowInsets(compose.activity.window.decorView))
            safeTopPx = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()).top
        }
        assertEquals(safeArea.top + gapPx, panel.top, 1f)
        assertTrue("WELCOME must be in the upper half", panel.top < screen.center.y)
        assertTrue("WELCOME must clear the status bar and cutout", panel.top >= safeTopPx + gapPx - 1f)
        assertTrue("WELCOME must not cover bottom navigation", panel.bottom <= navigation.top)
        assertTrue("Bob must be left of the welcome text", bob.right <= text.left)
        compose.onNodeWithTag("bottom_navigation", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("bob_coach_primary").assertIsDisplayed().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(home).fetchSemanticsNodes().size == 1 }
        val prefs = (compose.activity.application as ProjectADHDApplication).container.preferences
        assertEquals(OnboardingStep.HOME, runBlocking { prefs.onboarding.first().onboardingStep })
        val nextSafeArea = compose.onNodeWithTag("bob_coach_safe_area").fetchSemanticsNode().boundsInRoot
        val nextPanel = compose.onNodeWithTag("bob_coach_panel").fetchSemanticsNode().boundsInRoot
        assertEquals(nextSafeArea.bottom - gapPx, nextPanel.bottom, 1f)
        compose.onNodeWithTag("bob_coach_primary").assertIsDisplayed()
    }
}
