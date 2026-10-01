package com.ansa1r.projectadhd.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*

@Composable
fun BlockingSettingsScreen(timings: SettingsViewModel) {
    ScreenList { item { InterventionTimingSettings(timings) } }
}

@Composable
private fun InterventionTimingSettings(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val validCooldown = state.cooldown.toIntOrNull()?.let { it in 1..180 } == true
    val validPraise = state.praiseCooldown.toIntOrNull()?.let { it in 1..180 } == true
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MessageBanner(viewModel)
        SectionCard {
            Text(stringResource(R.string.cooldown), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.cooldown_hint))
            OutlinedTextField(value = state.cooldown, onValueChange = viewModel::change,
                label = { Text(stringResource(R.string.minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, isError = !validCooldown, enabled = !state.saving,
                supportingText = { Text(stringResource(R.string.minutes_range)) }, colors = brandFieldColors())
            Text(stringResource(R.string.saved_cooldown, state.savedCooldown))
            Text(stringResource(R.string.praise_cooldown), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.praise_cooldown_hint))
            OutlinedTextField(value = state.praiseCooldown, onValueChange = viewModel::changePraise,
                label = { Text(stringResource(R.string.minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, isError = !validPraise, enabled = !state.saving,
                supportingText = { Text(stringResource(R.string.minutes_range)) }, colors = brandFieldColors())
            Text(stringResource(R.string.saved_cooldown, state.savedPraiseCooldown))
            BrandButton(onClick = viewModel::save, enabled = validCooldown && validPraise && !state.saving,
                modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}
