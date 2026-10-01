package com.ansa1r.projectadhd.ui.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ansa1r.projectadhd.AppContainer
import com.ansa1r.projectadhd.domain.habits.HabitState
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.ui.mascot.HabitConfirmation
import com.ansa1r.projectadhd.ui.startup.LocalStartupVisible
import com.ansa1r.projectadhd.util.dayKey

@Composable
fun PendingHabitConfirmation(container: AppContainer, inlineOnHabits: Boolean) {
    val vm: HabitsViewModel = viewModel(key = "pending-habit-confirmation", factory = factory { HabitsViewModel(container) })
    val state by vm.state.collectAsStateWithLifecycle()
    val habit = state.habits.firstOrNull { it.isActive && it.progress.state == HabitState.AWAITING_CONFIRMATION }
    var dismissed by rememberSaveable { mutableStateOf<String?>(null) }
    val key = habit?.let { "${it.id}:${dayKey()}:${it.progress.extraTargetMinutes}" }
    if (habit != null && !inlineOnHabits && !LocalStartupVisible.current && key != dismissed) {
        Dialog(onDismissRequest = { dismissed = key }) {
            SectionCard {
                Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
                    MessageBanner(vm)
                    HabitConfirmation(habit, { vm.confirm(habit.id, true) }, { vm.confirm(habit.id, false) })
                    TextButton({ dismissed = key }) { Text("Позже") }
                }
            }
        }
    }
}
