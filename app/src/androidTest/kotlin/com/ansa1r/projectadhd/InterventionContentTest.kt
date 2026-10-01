package com.ansa1r.projectadhd

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ansa1r.projectadhd.domain.intervention.InterventionPayload
import com.ansa1r.projectadhd.ui.mascot.*
import com.ansa1r.projectadhd.ui.theme.ProjectADHDTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterventionContentTest {
    @get:Rule val compose = createComposeRule()
    @Test fun blockingContentHasTaskActionAndSuppliedMascot() {
        var opened = false
        compose.setContent { ProjectADHDTheme {
            BlockingContent(InterventionPayload("video.app", "Video", 60_000, 60_000, 2)) { opened = true }
        } }
        compose.onNodeWithContentDescription("Маскот просит вернуться к задачам").assertExists()
        compose.onNodeWithText("Осталось дел на сегодня: 2").assertExists()
        compose.onNodeWithText("Посмотреть дела").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
    @Test fun praiseHasNoBlockingTaskAction() {
        compose.setContent { ProjectADHDTheme { PraiseContent() } }
        compose.onNodeWithText("Отличная работа!").assertIsDisplayed()
        compose.onNodeWithText("Посмотреть дела").assertDoesNotExist()
        compose.onNodeWithContentDescription("Радостный маскот хвалит за выполненные дела").assertIsDisplayed()
    }
}
