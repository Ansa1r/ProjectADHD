package com.ansa1r.projectadhd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ansa1r.projectadhd.ui.onboarding.*
import com.ansa1r.projectadhd.ui.theme.ProjectADHDTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BobCoachOverlayTest {
    @get:Rule val compose = createComposeRule()
    @Test fun coachReservesScaffoldSpaceAndDoesNotMoveWhileTextAppears() {
        var visibleLength by mutableIntStateOf(0)
        var showCoach by mutableStateOf(true)
        val welcome = "Привет! Я Боб. Я помогу тебе следить за привычками и меньше отвлекаться."
        compose.setContent { ProjectADHDTheme {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val coachHeight = maxHeight * 0.4f
                Scaffold(bottomBar = {
                    Column {
                        if (showCoach) BobCoachPanel(welcome, "Продолжить", {},
                            modifier = Modifier.padding(8.dp).heightIn(max = coachHeight),
                            position = CoachPosition.BOTTOM, visibleTextLength = visibleLength)
                        NavigationBar(Modifier.testTag("coach_test_navigation")) {
                            listOf("Home", "Habits", "Apps", "Stats", "Bob").forEach { label ->
                                NavigationBarItem(selected = label == "Home", onClick = {}, icon = { Text(label) })
                            }
                        }
                    }
                }) { padding -> Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).testTag("coach_test_content")) }
            }
        } }
        val panel = compose.onNodeWithTag("bob_coach_panel").fetchSemanticsNode().boundsInRoot
        val navigation = compose.onNodeWithTag("coach_test_navigation").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("coach_test_content").fetchSemanticsNode().boundsInRoot
        val bob = compose.onNodeWithTag("bob_coach_mascot").fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithTag("bob_coach_text", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(panel.bottom <= navigation.top)
        assertTrue(content.bottom <= panel.top)
        assertTrue(text.right <= bob.left)
        for (length in listOf(welcome.length / 2, welcome.length)) {
            compose.runOnIdle { visibleLength = length }
            assertEquals(panel, compose.onNodeWithTag("bob_coach_panel").fetchSemanticsNode().boundsInRoot)
            assertEquals(navigation, compose.onNodeWithTag("coach_test_navigation").fetchSemanticsNode().boundsInRoot)
        }
        compose.runOnIdle { showCoach = false }
        compose.onNodeWithTag("bob_coach_panel").assertDoesNotExist()
        val expanded = compose.onNodeWithTag("coach_test_content").fetchSemanticsNode().boundsInRoot
        assertTrue(expanded.height > content.height)
        assertEquals(navigation, compose.onNodeWithTag("coach_test_navigation").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun primaryWorksAndCoachDoesNotActivateUnderlyingUi() {
        var underlying = 0; var continued = 0
        compose.setContent { ProjectADHDTheme { Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().testTag("underlying").clickable { underlying++ })
            BobCoachOverlay("Привет! Я Боб.", "Продолжить", { continued++ })
        } } }
        compose.onNodeWithTag("bob_coach_primary").performClick()
        compose.onNodeWithTag("underlying").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, continued); assertEquals(0, underlying) }
    }
    @Test fun topBobIsLeftAndBottomBobIsRightWithReadableText() {
        var position by mutableStateOf(CoachPosition.TOP)
        compose.setContent { ProjectADHDTheme {
            BobCoachOverlay("Здесь твой прогресс.", "Продолжить", {}, position = position)
        } }
        var bob = compose.onNodeWithTag("bob_coach_mascot").fetchSemanticsNode().boundsInRoot
        var text = compose.onNodeWithText("Здесь твой прогресс.").fetchSemanticsNode().boundsInRoot
        assertTrue(bob.right <= text.left)
        compose.runOnIdle { position = CoachPosition.BOTTOM }
        bob = compose.onNodeWithTag("bob_coach_mascot").fetchSemanticsNode().boundsInRoot
        text = compose.onNodeWithText("Здесь твой прогресс.").fetchSemanticsNode().boundsInRoot
        assertTrue(text.right <= bob.left)
        compose.onNodeWithTag("bob_coach_primary").assertIsDisplayed()
    }
}
