package com.ansa1r.projectadhd.ui.mascot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import com.ansa1r.projectadhd.domain.model.Habit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.intervention.InterventionPayload
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.ui.components.BrandButton
import com.ansa1r.projectadhd.ui.theme.BrandColors

@Composable
fun BlockingScrim(modifier: Modifier = Modifier) {
    Box(modifier.background(BrandColors.BlockingScrim.copy(alpha = 0.85f)))
}

@Composable
fun BlockingContent(
    payload: InterventionPayload,
    test: Boolean = false,
    confirmation: Habit? = null,
    confirm: (Long, Boolean) -> Unit = { _, _ -> },
    openHabits: () -> Unit
) {
    // This root is transparent. Only the separate scrim layer has fixed 0.85 alpha.
    Box(Modifier.fillMaxSize().clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
        BlockingScrim(Modifier.matchParentSize())
        val shadow = Shadow(BrandColors.Background, Offset(0f, 2f), 6f)
        Column(Modifier.align(Alignment.Center).widthIn(max = 480.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (confirmation != null && !test) {
                HabitConfirmation(confirmation, onYes = { confirm(confirmation.id, true) }, onNo = { confirm(confirmation.id, false) }, opaqueButtons = true)
            } else {
            MascotView(MascotMood.BLOCKING, Modifier.size(180.dp))
            Text(stringResource(R.string.block_title), color = BrandColors.Text,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, shadow = shadow),
                textAlign = TextAlign.Center)
            Text(payload.appName, color = BrandColors.Text,
                style = MaterialTheme.typography.titleMedium.copy(shadow = shadow), textAlign = TextAlign.Center)
            Text(stringResource(R.string.block_elapsed, (payload.sessionDurationMillis / 60_000L).coerceAtLeast(0)),
                color = BrandColors.Text, style = MaterialTheme.typography.titleMedium.copy(shadow = shadow),
                textAlign = TextAlign.Center)
            Text(stringResource(R.string.tasks_remaining, payload.incompleteHabitCount), color = BrandColors.Text,
                style = MaterialTheme.typography.titleMedium.copy(shadow = shadow), textAlign = TextAlign.Center)
            Text(stringResource(R.string.block_instruction), color = BrandColors.Text,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, shadow = shadow),
                textAlign = TextAlign.Center)
            BrandButton(onClick = openHabits, modifier = Modifier.fillMaxWidth().testTag("blocking_task_button"), opaque = true) {
                Text(stringResource(R.string.go_to_tasks), textAlign = TextAlign.Center)
            }
            }
            if (test) Text(stringResource(R.string.debug_block_timeout), color = BrandColors.Text,
                style = MaterialTheme.typography.bodySmall.copy(shadow = shadow), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PraiseContent(test: Boolean = false) {
    Surface(Modifier.padding(12.dp), shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, BrandColors.PurpleOutline), color = BrandColors.PurpleDeep.copy(alpha = 0.85f)) {
        MascotBackdrop {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MascotView(MascotMood.PRAISE, Modifier.size(96.dp))
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
                    color = BrandColors.PurpleDeep.copy(alpha = 0.85f), contentColor = BrandColors.Text) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.praise_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.praise_body), style = MaterialTheme.typography.bodyMedium)
                        if (test) Text(stringResource(R.string.debug_praise_test), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
