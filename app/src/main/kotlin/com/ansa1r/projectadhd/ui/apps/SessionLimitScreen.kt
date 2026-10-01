package com.ansa1r.projectadhd.ui.apps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun SessionLimitScreen(viewModel: AppsViewModel, saved: () -> Unit, returnToSelection: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) saved() }
    val rows = state.draft?.selected?.values.orEmpty().sortedBy { it.app.displayName.lowercase() }
    Column(Modifier.fillMaxSize().imePadding().testTag("session_limits")) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MessageBanner(viewModel) }
            if (state.draft == null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (state.draft != null && rows.isEmpty()) item {
                Text(stringResource(R.string.selection_missing))
                OutlinedButton(onClick = returnToSelection) { Text(stringResource(R.string.choose_apps)) }
            }
            items(rows, key = { it.app.packageName }) { row ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InstalledAppIcon(row.app.packageName, state.installed.any { it.packageName == row.app.packageName },
                            state.iconsRevision, viewModel.icons)
                        Text(row.app.displayName, style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(value = row.input, onValueChange = { viewModel.limitChanged(row.app.packageName, it) },
                        label = { Text(stringResource(R.string.limit_for_app, row.app.displayName)) },
                        singleLine = true, isError = row.minutes == null, enabled = !state.saving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text(stringResource(R.string.minutes_range)) },
                        modifier = Modifier.fillMaxWidth().testTag("limit_" + row.app.packageName))
                }
            }
            if (rows.isNotEmpty()) item {
                SectionCard {
                    val valid = state.commonLimit.toIntOrNull()?.let { it in 1..180 } == true
                    OutlinedTextField(state.commonLimit, viewModel::commonLimitChanged,
                        label = { Text(stringResource(R.string.common_limit)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text(stringResource(R.string.minutes_range)) },
                        isError = !valid, enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("common_limit"))
                    OutlinedButton(viewModel::applyToAll, enabled = valid && !state.saving, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.apply_to_all))
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.background) {
            BrandButton(viewModel::save, enabled = rows.isNotEmpty() && state.draft?.valid == true && !state.saving && !state.saved,
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("save_selection")) {
                Text(stringResource(if (state.saving) R.string.saving else R.string.save))
            }
        }
    }
}
