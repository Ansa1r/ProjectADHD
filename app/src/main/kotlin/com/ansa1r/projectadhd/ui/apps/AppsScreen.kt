package com.ansa1r.projectadhd.ui.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun AppsScreen(viewModel: AppsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    ScreenList {
        item { MessageBanner(viewModel) }
        item { Text(stringResource(R.string.apps_intro)) }
        item {
            OutlinedTextField(value = state.query, onValueChange = viewModel::search,
                label = { Text(stringResource(R.string.search_apps)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item { OutlinedButton(onClick = viewModel::refresh, enabled = !state.loading) { Text(stringResource(R.string.refresh_list)) } }
        if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!state.loading && state.rows.isEmpty()) item { Text(stringResource(R.string.apps_empty)) }
        items(state.rows, key = { it.installed.packageName }) { row ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InstalledAppIcon(row.installed.packageName, row.available, state.iconsRevision, viewModel.icons)
                    Column(Modifier.weight(1f)) {
                        Text(row.installed.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(row.installed.packageName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Checkbox(checked = row.tracked?.enabled == true,
                        onCheckedChange = { viewModel.select(row, it) },
                        enabled = row.available || row.tracked?.enabled == true)
                }
                Text(stringResource(if (row.tracked?.enabled == true) R.string.tracking_enabled else R.string.tracking_disabled))
                Text(stringResource(R.string.session_limit_value, row.tracked?.sessionLimitMinutes ?: 15))
                row.tracked?.let { tracked ->
                    TextButton(onClick = { viewModel.editLimit(tracked) }) { Text(stringResource(R.string.change_limit)) }
                }
                if (!row.available) {
                    Text(stringResource(R.string.app_unavailable))
                    TextButton(onClick = { viewModel.remove(row.installed.packageName) }) { Text(stringResource(R.string.remove_from_list)) }
                }
            }
        }
    }
    if (state.editingPackage != null) {
        val valid = state.limitInput.toIntOrNull()?.let { it in 1..180 } == true
        AlertDialog(onDismissRequest = viewModel::closeEditor,
            title = { Text(stringResource(R.string.session_limit)) },
            text = {
                OutlinedTextField(value = state.limitInput, onValueChange = viewModel::limitChanged,
                    label = { Text(stringResource(R.string.minutes)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text(stringResource(R.string.minutes_range)) },
                    isError = !valid, enabled = !state.saving)
            },
            confirmButton = { TextButton(onClick = viewModel::saveLimit, enabled = valid && !state.saving) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton(onClick = viewModel::closeEditor, enabled = !state.saving) { Text(stringResource(R.string.cancel)) } })
    }
}
