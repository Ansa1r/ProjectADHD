package com.ansa1r.projectadhd.ui.mascot

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
import com.ansa1r.projectadhd.util.durationText

@Composable
fun BlockingContent(payload: InterventionPayload, test: Boolean = false, openHabits: () -> Unit) {
    MascotBackdrop(Modifier.fillMaxSize().clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})) {
        Column(Modifier.align(Alignment.Center).widthIn(max = 480.dp).fillMaxWidth()
            .verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MascotView(MascotMood.BLOCKING, Modifier.size(200.dp))
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(stringResource(R.string.block_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.block_app, payload.appName, durationText(payload.sessionDurationMillis)), textAlign = TextAlign.Center)
                    Text(stringResource(R.string.tasks_remaining, payload.incompleteHabitCount), color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.block_instruction), textAlign = TextAlign.Center)
                    Button(onClick = openHabits, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.go_to_tasks)) }
                    if (test) Text(stringResource(R.string.debug_block_timeout), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PraiseContent(test: Boolean = false) {
    Surface(Modifier.padding(12.dp), shape = RoundedCornerShape(24.dp)) {
        MascotBackdrop {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MascotView(MascotMood.PRAISE, Modifier.size(96.dp))
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
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
