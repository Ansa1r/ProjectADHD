package com.ansa1r.projectadhd.ui.mascot

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.intervention.InterventionPayload
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.ui.components.BrandButton
import com.ansa1r.projectadhd.ui.theme.BrandColors

@Composable
fun BlockingContent(payload: InterventionPayload, test: Boolean = false, openHabits: () -> Unit) {
    MascotBackdrop(Modifier.fillMaxSize().clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
        // Scrolling keeps the action reachable in landscape and with enlarged system fonts.
        Column(Modifier.align(Alignment.Center).widthIn(max = 480.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(32.dp), color = BrandColors.PurpleDeep,
                contentColor = BrandColors.Text, border = BorderStroke(3.dp, BrandColors.Green)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    MascotView(MascotMood.BLOCKING, Modifier.size(180.dp))
                    Text(stringResource(R.string.block_title), style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center)
                    Text(payload.appName, style = MaterialTheme.typography.titleMedium,
                        color = BrandColors.Tertiary, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.block_elapsed, (payload.sessionDurationMillis / 60_000L).coerceAtLeast(0)),
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.tasks_remaining, payload.incompleteHabitCount),
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.block_instruction), style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center)
                    BrandButton(onClick = openHabits, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.go_to_tasks), textAlign = TextAlign.Center)
                    }
                    if (test) Text(stringResource(R.string.debug_block_timeout),
                        style = MaterialTheme.typography.bodySmall, color = BrandColors.Muted, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun PraiseContent(test: Boolean = false) {
    Surface(Modifier.padding(12.dp), shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, BrandColors.PurpleOutline), color = BrandColors.PurpleDeep) {
        MascotBackdrop {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MascotView(MascotMood.PRAISE, Modifier.size(96.dp))
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
                    color = BrandColors.PurpleDeep.copy(alpha = 0.96f), contentColor = BrandColors.Text) {
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
