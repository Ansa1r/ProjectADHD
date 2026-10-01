package com.ansa1r.projectadhd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ansa1r.projectadhd.domain.intervention.InterventionPayload
import com.ansa1r.projectadhd.ui.mascot.BlockingContent
import com.ansa1r.projectadhd.ui.theme.BrandColors
import com.ansa1r.projectadhd.ui.theme.ProjectADHDTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockingOpacityContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scrimBlendsWithUnderlyingContentButButtonStaysOpaque() {
        val percent = mutableIntStateOf(30)
        compose.setContent {
            ProjectADHDTheme {
                Box(Modifier.fillMaxSize().background(Color.White).testTag("scene")) {
                    BlockingContent(InterventionPayload("test.app", "Test", 60_000, 60_000, 2),
                        opacityPercent = percent.intValue, openHabits = {})
                }
            }
        }
        compose.onNodeWithText("Посмотреть дела").performScrollTo()
        for (value in listOf(30, 65, 90)) {
            compose.runOnIdle { percent.intValue = value }
            val pixels = compose.onNodeWithTag("scene").captureToImage().toPixelMap()
            val alpha = value / 100f
            val background = pixels[1, 1]
            assertEquals(1f - alpha + BrandColors.BlockingScrim.red * alpha, background.red, 0.025f)
            assertEquals(1f - alpha + BrandColors.BlockingScrim.green * alpha, background.green, 0.025f)
            assertEquals(1f - alpha + BrandColors.BlockingScrim.blue * alpha, background.blue, 0.025f)
            val button = compose.onNodeWithTag("blocking_task_button").captureToImage().toPixelMap()
            val fill = button[button.width / 2, button.height - 8]
            assertEquals(BrandColors.PurpleAction.red, fill.red, 0.025f)
            assertEquals(BrandColors.PurpleAction.green, fill.green, 0.025f)
            assertEquals(BrandColors.PurpleAction.blue, fill.blue, 0.025f)
        }
    }
}
