package com.ansa1r.projectadhd.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val valid = state.cooldown.toIntOrNull()?.let { it in 1..180 } == true &&
        state.praiseCooldown.toIntOrNull()?.let { it in 1..180 } == true
    ScreenList {
        item { MessageBanner(viewModel) }
        item {
            SectionCard {
                Text(stringResource(R.string.cooldown), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.cooldown_hint))
                OutlinedTextField(value = state.cooldown, onValueChange = viewModel::change,
                    label = { Text(stringResource(R.string.minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, isError = !valid, enabled = !state.saving,
                    supportingText = { Text(stringResource(R.string.minutes_range)) })
                Text(stringResource(R.string.saved_cooldown, state.savedCooldown))
                Text(stringResource(R.string.praise_cooldown), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.praise_cooldown_hint))
                OutlinedTextField(value = state.praiseCooldown, onValueChange = viewModel::changePraise,
                    label = { Text(stringResource(R.string.minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, enabled = !state.saving,
                    isError = state.praiseCooldown.toIntOrNull()?.let { it in 1..180 } != true,
                    supportingText = { Text(stringResource(R.string.minutes_range)) })
                Text(stringResource(R.string.saved_cooldown, state.savedPraiseCooldown))
                BrandButton(onClick = viewModel::save, enabled = valid && !state.saving,
                    modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
            }
        }
        item {
            SectionCard {
                Text(stringResource(R.string.emergency_stop_hint))
                OutlinedButton(onClick = viewModel::stop, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stop_monitoring))
                }
            }
        }
        item {
            SectionCard {
                Text(stringResource(R.string.local_data_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.local_data_body))
                Text(stringResource(R.string.stage2_limitations))
            }
        }
    }
}
