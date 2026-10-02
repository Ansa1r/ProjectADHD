package com.ansa1r.projectadhd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
