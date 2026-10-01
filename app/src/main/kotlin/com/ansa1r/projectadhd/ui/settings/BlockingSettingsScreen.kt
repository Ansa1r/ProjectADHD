package com.ansa1r.projectadhd.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.domain.settings.BlockingOpacity
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.ui.mascot.BlockingScrim
import com.ansa1r.projectadhd.ui.mascot.MascotView
import com.ansa1r.projectadhd.ui.theme.BrandColors

@Composable
fun BlockingSettingsScreen(viewModel: BlockingSettingsViewModel, timings: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val label = stringResource(R.string.blocking_opacity)
    ScreenList {
        item { MessageBanner(viewModel) }
        item {
            SectionCard {
                Text(label, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.opacity_explanation))
                Text(stringResource(R.string.percent_value, state.percent), style = MaterialTheme.typography.headlineSmall,
                    color = BrandColors.Primary, modifier = Modifier.testTag("opacity_value"))
                Slider(value = state.percent.toFloat(), onValueChange = viewModel::change,
                    enabled = !state.loading,
                    valueRange = BlockingOpacity.MIN_PERCENT.toFloat()..BlockingOpacity.MAX_PERCENT.toFloat(),
                    steps = BlockingOpacity.SLIDER_STEPS,
                    modifier = Modifier.fillMaxWidth().testTag("opacity_slider").semantics { contentDescription = label })
                Text(stringResource(if (state.loading) R.string.loading else if (state.saving) R.string.opacity_saving else R.string.opacity_saved),
                    style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.opacity_preview), style = MaterialTheme.typography.titleMedium)
                BlockingOpacityPreview(state.percent)
            }
        }
        item { InterventionTimingSettings(timings) }
    }
}

@Composable
fun BlockingOpacityPreview(percent: Int) {
    Box(Modifier.fillMaxWidth().height(248.dp).clip(RoundedCornerShape(18.dp)).testTag("opacity_preview")) {
        Column(Modifier.fillMaxSize().background(BrandColors.PreviewSurface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.preview_content), color = BrandColors.PreviewInk,
                style = MaterialTheme.typography.titleSmall)
            Box(Modifier.fillMaxWidth().height(90.dp).background(BrandColors.Tertiary, RoundedCornerShape(10.dp)))
            Box(Modifier.fillMaxWidth(0.8f).height(10.dp).background(BrandColors.PreviewInk))
            Box(Modifier.fillMaxWidth(0.6f).height(10.dp).background(BrandColors.PreviewInk))
        }
        BlockingScrim(percent, Modifier.matchParentSize())
        Column(Modifier.align(Alignment.Center).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MascotView(MascotMood.BLOCKING, Modifier.size(88.dp))
            Text(stringResource(R.string.block_title), color = BrandColors.Text, style = MaterialTheme.typography.titleMedium)
            Surface(shape = RoundedCornerShape(14.dp), color = BrandColors.PurpleAction, contentColor = BrandColors.Text) {
                Text(stringResource(R.string.go_to_tasks), Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
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
                supportingText = { Text(stringResource(R.string.minutes_range)) })
            Text(stringResource(R.string.saved_cooldown, state.savedCooldown))
            Text(stringResource(R.string.praise_cooldown), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.praise_cooldown_hint))
            OutlinedTextField(value = state.praiseCooldown, onValueChange = viewModel::changePraise,
                label = { Text(stringResource(R.string.minutes)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, isError = !validPraise, enabled = !state.saving,
                supportingText = { Text(stringResource(R.string.minutes_range)) })
            Text(stringResource(R.string.saved_cooldown, state.savedPraiseCooldown))
            BrandButton(onClick = viewModel::save, enabled = validCooldown && validPraise && !state.saving,
                modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}
