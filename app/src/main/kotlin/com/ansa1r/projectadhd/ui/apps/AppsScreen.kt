package com.ansa1r.projectadhd.ui.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun AppsScreen(viewModel: AppsViewModel, continueToLimits: () -> Unit, saved: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) saved() }
    RefreshOnResume(viewModel::refresh)
    Column(Modifier.fillMaxSize().testTag("app_selection")) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MessageBanner(viewModel) }
            item {
                OutlinedTextField(value = state.query, onValueChange = viewModel::search,
                    label = { Text(stringResource(R.string.search_apps)) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("app_search"), colors = brandFieldColors())
            }
            item { BrandOutlinedButton(onClick = viewModel::refresh, enabled = !state.loading) { Text(stringResource(R.string.refresh_list)) } }
            if (state.loading || state.draft == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (!state.loading && state.rows.isEmpty()) item { Text(stringResource(R.string.apps_empty)) }
            items(state.rows, key = { it.installed.packageName }) { row ->
                SectionCard {
                    Row(Modifier.fillMaxWidth().testTag("app_choice_" + row.installed.packageName)
                        .toggleable(value = row.selected, role = Role.Checkbox,
                            enabled = state.draft != null && !state.saving && ((row.available && !row.habitLinked) || row.selected),
                            onValueChange = { viewModel.select(row, it) }),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InstalledAppIcon(row.installed.packageName, row.available, state.iconsRevision, viewModel.icons)
                        Column(Modifier.weight(1f)) {
                            Text(row.installed.displayName, style = MaterialTheme.typography.titleMedium)
                            if (row.habitLinked) Text("Используется в привычке")
                            if (row.habitLinked && row.selected) Text("Конфликт: снимите ограничение. Блокировка отключена.")
                        }
                        Checkbox(checked = row.selected, onCheckedChange = null, enabled = !row.habitLinked || row.selected)
                    }
                }
            }
        }
        val draft = state.draft
        if (draft != null && (draft.selectionChanged || draft.selected.isNotEmpty())) {
            val empty = draft.selected.isEmpty()
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (draft.selectionChanged) BrandButton(
                        onClick = { if (empty) viewModel.save() else continueToLimits() },
                        enabled = !state.saving && !state.saved, modifier = Modifier.fillMaxWidth().testTag("selection_action")) {
                        Text(stringResource(if (state.saving) R.string.saving else if (empty) R.string.save else R.string.continue_selection))
                    } else BrandOutlinedButton(onClick = continueToLimits, enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().testTag("edit_limits")) { Text(stringResource(R.string.configure_limits)) }
                }
            }
        }
    }
}
