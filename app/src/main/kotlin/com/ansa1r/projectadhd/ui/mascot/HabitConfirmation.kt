package com.ansa1r.projectadhd.ui.mascot

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.ansa1r.projectadhd.domain.model.*
import com.ansa1r.projectadhd.ui.components.BrandButton
import com.ansa1r.projectadhd.util.durationText
import com.ansa1r.projectadhd.ui.theme.BrandColors

@Composable
fun HabitConfirmation(habit: Habit, onYes: () -> Unit, onNo: () -> Unit, opaqueButtons: Boolean = false) {
    Column(Modifier.fillMaxWidth().testTag("habit_confirmation_${habit.id}"), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MascotView(MascotMood.IDLE, Modifier.size(140.dp))
        Text("Получилось?", color = BrandColors.Text, style = MaterialTheme.typography.headlineSmall)
        Text(habit.title, color = BrandColors.Text, style = MaterialTheme.typography.titleLarge)
        Text("Требуемое время: " + durationText(habit.progress.targetMillis(habit.targetDurationMinutes)), color = BrandColors.Text)
        Text("Подтверди, что ты занимался делом.", color = BrandColors.Text)
        BrandButton(onYes, Modifier.fillMaxWidth(), opaque = opaqueButtons) { Text("Да") }
        BrandButton(onNo, Modifier.fillMaxWidth(), opaque = opaqueButtons) { Text("Нет · ещё 10 минут") }
    }
}
