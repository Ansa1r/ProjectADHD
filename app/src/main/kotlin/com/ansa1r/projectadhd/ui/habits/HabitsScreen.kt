package com.ansa1r.projectadhd.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun HabitsScreen(viewModel: HabitsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var deleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    ScreenList {
        item { MessageBanner(viewModel) }
        item {
            BrandButton(onClick = { viewModel.edit() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_habit)) }
        }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!state.loading && state.habits.isEmpty()) item { Text(stringResource(R.string.habits_empty)) }
        items(state.habits, key = { it.id }) { habit ->
            SectionCard {
                Text(habit.title, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = habit.completedToday, enabled = habit.isActive,
                        onCheckedChange = { viewModel.completed(habit, it) })
                    Text(stringResource(R.string.completed_today), modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.habit_active), modifier = Modifier.weight(1f))
                    Switch(checked = habit.isActive, onCheckedChange = { viewModel.active(habit, it) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.edit(habit) }) { Text(stringResource(R.string.edit)) }
                    TextButton(onClick = { deleteId = habit.id }) { Text(stringResource(R.string.delete)) }
                }
            }
        }
    }
    state.draft?.let { draft ->
        AlertDialog(onDismissRequest = viewModel::closeEditor,
            title = { Text(stringResource(if (draft.id == null) R.string.add_habit else R.string.edit_habit)) },
            text = {
                OutlinedTextField(value = draft.title, onValueChange = viewModel::titleChanged,
                    label = { Text(stringResource(R.string.habit_title)) },
                    supportingText = { Text(stringResource(R.string.habit_title_hint)) },
                    singleLine = true, enabled = !state.saving)
            },
            confirmButton = { TextButton(onClick = viewModel::save, enabled = draft.title.isNotBlank() && !state.saving) {
                Text(stringResource(R.string.save))
            } },
            dismissButton = { TextButton(onClick = viewModel::closeEditor, enabled = !state.saving) {
                Text(stringResource(R.string.cancel))
            } })
    }
    deleteId?.let { id ->
        AlertDialog(onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.delete_habit_title)) },
            text = { Text(stringResource(R.string.delete_habit_body)) },
            confirmButton = { TextButton(onClick = { viewModel.delete(id); deleteId = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) } })
    }
}
