package com.ansa1r.projectadhd.ui.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.habits.*
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.ui.mascot.HabitConfirmation
import com.ansa1r.projectadhd.util.durationText

@Composable
fun HabitsScreen(viewModel: HabitsViewModel, create: () -> Unit, edit: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    RefreshOnResume(viewModel::refresh)
    ScreenList {
        item { MessageBanner(viewModel) }
        item { BrandButton(create, Modifier.fillMaxWidth().testTag("add_habit")) { Text(stringResource(R.string.add_habit)) } }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!state.loading && state.habits.isEmpty()) item { Text(stringResource(R.string.habits_empty)) }
        if (state.habits.any { it.type == HabitType.APP_BASED && it.isActive } && (!state.usageAllowed || !state.liveTracking)) item {
            SectionCard { Text(if (!state.usageAllowed) "Для привычек с приложением разрешите доступ к статистике использования в настройках."
                else "Для автоматического выполнения в фоне включите мониторинг на Home. После остановки прогресс уточняется при возвращении в приложение.") }
        }
        items(state.habits, key = { it.id }) { habit ->
            SectionCard {
                Text(habit.title, style = MaterialTheme.typography.titleLarge)
                Text(if (habit.type == HabitType.MANUAL) "Без приложения" else "Приложение: ${habit.linkedAppPackage}")
                val p = habit.progress
                val target = p.targetMillis(habit.targetDurationMinutes)
                if (habit.legacyCompletion) Text("Выполнено в прежней версии. Время не записывалось.") else {
                Text("${durationText(p.accumulatedMillis)} / ${durationText(target)}", modifier = Modifier.testTag("habit_progress_${habit.id}"))
                LinearProgressIndicator(progress = { (p.accumulatedMillis.toFloat() / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                }
                Text(when (p.state) {
                    HabitState.READY -> "Можно начинать"; HabitState.IN_PROGRESS -> "В процессе"
                    HabitState.PAUSED -> "Пауза"; HabitState.AWAITING_CONFIRMATION -> "Ждёт подтверждения"; HabitState.COMPLETED -> "Выполнено сегодня"
                })
                if (habit.linkedAppPackage?.let { it in state.conflicts } == true) Text("Конфликт: приложение также ограничено. Блокировка этого приложения отключена. Уберите его из ограничений в Apps.")
                if (habit.isActive && !habit.completedToday) {
                    if (habit.type == HabitType.MANUAL) {
                        Text("Текущая сессия: " + durationText(p.sessionMillis))
                        when (p.state) {
                            HabitState.AWAITING_CONFIRMATION -> HabitConfirmation(habit, { viewModel.confirm(habit.id, true) }, { viewModel.confirm(habit.id, false) })
                            HabitState.IN_PROGRESS -> BrandButton({ viewModel.pause(habit.id) }, Modifier.fillMaxWidth()) { Text("Пауза") }
                            HabitState.READY, HabitState.PAUSED -> BrandButton({ viewModel.start(habit.id) }, Modifier.fillMaxWidth()) { Text(if (p.state == HabitState.READY) "Начать" else "Продолжить") }
                            HabitState.COMPLETED -> Unit
                        }
                    } else BrandButton({ viewModel.openApp(context, habit) }, Modifier.fillMaxWidth()) { Text("Открыть приложение") }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.habit_active), Modifier.weight(1f))
                    Switch(habit.isActive, { viewModel.active(habit, it) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton({ edit(habit.id) }) { Text(stringResource(R.string.edit)) }
                    TextButton({ deleteId = habit.id }) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
    deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Удалить привычку?") },
        text = { Text("Привычка и её дневные записи будут удалены. Уже полученный XP и история наград сохранятся.") },
        confirmButton = { TextButton({ viewModel.delete(id); deleteId = null }) { Text("Удалить") } },
        dismissButton = { TextButton({ deleteId = null }) { Text("Отмена") } }) }
}
