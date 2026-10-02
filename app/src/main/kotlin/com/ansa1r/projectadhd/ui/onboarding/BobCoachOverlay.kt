package com.ansa1r.projectadhd.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.ui.components.BrandButton
import com.ansa1r.projectadhd.ui.mascot.MascotView
import com.ansa1r.projectadhd.ui.theme.BrandColors
import com.ansa1r.projectadhd.ui.theme.BrandOpacity

val LocalCoachStep = androidx.compose.runtime.staticCompositionLocalOf<com.ansa1r.projectadhd.domain.onboarding.OnboardingStep?> { null }

enum class CoachPosition { TOP, BOTTOM }

/** Shared by the tour and Settings confirmation. Only the purple fill is translucent. */
@Composable
fun BobCoachPanel(text: String, actionLabel: String, action: () -> Unit,
    modifier: Modifier = Modifier, position: CoachPosition = CoachPosition.BOTTOM,
    secondaryLabel: String? = null, secondary: () -> Unit = {}, enabled: Boolean = true,
    mood: MascotMood = MascotMood.IDLE) {
    Surface(modifier.fillMaxWidth().testTag("bob_coach_panel").semantics { paneTitle = "Боб"; liveRegion = LiveRegionMode.Polite }
        .pointerInput(Unit) { detectTapGestures { } }, shape = RoundedCornerShape(20.dp),
        color = BrandColors.PurpleDeep.copy(alpha = BrandOpacity.Coach), contentColor = BrandColors.Text,
        border = BorderStroke(2.dp, BrandColors.PurpleOutline), tonalElevation = 0.dp) {
        Row(Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (position == CoachPosition.TOP) MascotView(mood, Modifier.size(72.dp).testTag("bob_coach_mascot"))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = BrandColors.Text)
                BrandButton(action, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bob_coach_primary"), enabled = enabled, opaque = true) {
                    Text(actionLabel, color = BrandColors.Text)
                }
                if (secondaryLabel != null) TextButton(secondary, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bob_coach_secondary")) {
                    Text(secondaryLabel, color = BrandColors.Text)
                }
            }
            if (position == CoachPosition.BOTTOM) MascotView(mood, Modifier.size(72.dp).testTag("bob_coach_mascot"))
        }
    }
}

/** Real app content stays visible. Input outside the panel can be locked for a guided step. */
@Composable
fun BobCoachOverlay(text: String, actionLabel: String, action: () -> Unit,
    position: CoachPosition = CoachPosition.BOTTOM, secondaryLabel: String? = null,
    secondary: () -> Unit = {}, enabled: Boolean = true, dismissAllowed: Boolean = false,
    onDismiss: () -> Unit = {}, mood: MascotMood = MascotMood.IDLE) {
    BackHandler { if (dismissAllowed) onDismiss() }
    val safeInsets = if (position == CoachPosition.TOP) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else WindowInsets.safeDrawing
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(safeInsets)
            .then(if (position == CoachPosition.BOTTOM) Modifier.imePadding() else Modifier)
            .testTag("bob_coach_safe_area")
            .pointerInput(Unit) { detectTapGestures { if (dismissAllowed) onDismiss() } },
        contentAlignment = when (position) {
            CoachPosition.TOP -> Alignment.TopCenter
            CoachPosition.BOTTOM -> Alignment.BottomCenter
        }
    ) {
        BobCoachPanel(text, actionLabel, action,
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp).heightIn(max = maxHeight * 0.48f),
            position, secondaryLabel, secondary, enabled, mood)
    }
}
