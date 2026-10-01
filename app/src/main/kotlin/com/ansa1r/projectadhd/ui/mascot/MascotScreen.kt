package com.ansa1r.projectadhd.ui.mascot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.mascot.MascotProgression
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.domain.profile.Nickname
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun MascotScreen(vm: MascotViewModel, customize: () -> Unit, history: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    val xp = state.progress.totalXp
    val level = MascotProgression.level(xp)
    val floor = if (level == 1) 0L else MascotProgression.threshold(level - 1)
    val ceiling = MascotProgression.threshold(level)
    ScreenList {
        item { MessageBanner(vm) }
        item { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            MascotView(MascotMood.IDLE, Modifier.size(224.dp))
            Text(state.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("mascot_name"))
            Text("Уровень $level", style = MaterialTheme.typography.titleLarge)
            TextButton({ name = state.name; editing = true }) { Text("Изменить имя") }
        } }
        item { SectionCard {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("${xp - floor} / ${ceiling - floor} XP", style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(progress = { ((xp - floor).toDouble() / (ceiling - floor)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("Всего опыта: $xp XP")
            Text("Выполнено привычек: ${state.progress.completedHabits}")
            Text("Серия: ${state.progress.streak} дней")
        } }
        item { MenuCard("Кастомизация", "Внешний вид Боба", R.drawable.ic_theme, customize) }
        item { MenuCard("История развития", "Полученные награды", R.drawable.ic_nav_stats, history) }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("Имя маскота") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, colors = brandFieldColors(), supportingText = { Text("От 1 до 32 символов") }) },
        confirmButton = { TextButton({ vm.rename(name); editing = false }, enabled = Nickname.valid(name)) { Text("Сохранить") } },
        dismissButton = { TextButton({ editing = false }) { Text("Отмена") } })
}

@Composable
fun MascotHistoryScreen(vm: MascotViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    ScreenList {
        item { MessageBanner(vm) }
        if (state.history.isEmpty()) item { SectionCard { Text("Здесь появятся награды за выполненные привычки и дни.") } }
        items(state.history, key = { it.eventKey }) { award -> SectionCard {
            Text(if (award.kind == "HABIT") "Привычка выполнена" else "Все дела дня выполнены", style = MaterialTheme.typography.titleMedium)
            Text("+${award.awardedXp} XP · ${award.localDate}")
            Text("Уровень при получении: ${award.levelBefore}")
        } }
        if (state.history.size == 200) item { Text("Показаны последние 200 наград. Весь опыт сохранён.") }
    }
}
