package com.ansa1r.projectadhd.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.BuildConfig
import com.ansa1r.projectadhd.ui.components.SectionCard
import com.ansa1r.projectadhd.ui.mascot.animation.BobAnimationState
import com.ansa1r.projectadhd.ui.mascot.animation.BobMascot

@Composable
fun BobAnimationPreview() {
    if (!BuildConfig.DEBUG) return
    var state by remember { mutableStateOf(BobAnimationState.IDLE) }
    var talking by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(false) }
    var blink by remember { mutableIntStateOf(0) }
    SectionCard {
        Text("Bob Animation Preview", style = MaterialTheme.typography.titleLarge)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BobMascot(Modifier.size(240.dp).testTag("bob_animation_preview"), state = state,
                isTalking = talking, blinkRequest = blink, showLayerBounds = bounds)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((value, label) in listOf(BobAnimationState.IDLE to "Idle",
                BobAnimationState.BLOCKING to "Blocking", BobAnimationState.HAPPY to "Happy")) {
                FilterChip(selected = state == value, onClick = { state = value }, label = { Text(label) })
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Talking", Modifier.weight(1f))
            Switch(checked = talking, onCheckedChange = { talking = it })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Границы слоёв и pivots", Modifier.weight(1f))
            Switch(checked = bounds, onCheckedChange = { bounds = it })
        }
        OutlinedButton(onClick = { blink++ }) { Text("Моргнуть сейчас") }
        Text("Тело, волосы, руки, четыре части нижнего края, шарф и лицо — отдельные векторные слои.")
    }
}
