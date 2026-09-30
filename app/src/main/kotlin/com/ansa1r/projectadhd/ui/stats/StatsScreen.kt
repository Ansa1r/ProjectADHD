package com.ansa1r.projectadhd.ui.stats

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.util.durationText
import com.ansa1r.projectadhd.util.timestampText

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    ScreenList {
        item { MessageBanner(viewModel) }
        item {
            OutlinedButton(onClick = viewModel::refresh, enabled = !state.refreshing) { Text(stringResource(R.string.refresh)) }
            if (state.refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        item {
            Text(stringResource(R.string.usage_today), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.usage_estimate_hint), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.updated_value, timestampText(state.updatedAt)))
        }
        if (state.apps.isEmpty()) item { Text(stringResource(R.string.tracked_apps_empty)) }
        items(state.apps, key = { "app:" + it.packageName }) { app ->
            SectionCard {
                Text(app.displayName, style = MaterialTheme.typography.titleMedium)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                Detail(stringResource(R.string.usage_today), if (state.usageAvailable) {
                    durationText(state.usage[app.packageName] ?: 0)
                } else stringResource(R.string.unavailable))
                Text(stringResource(R.string.session_limit_value, app.sessionLimitMinutes))
                if (!app.enabled) Text(stringResource(R.string.tracking_disabled))
            }
        }
        item {
            Text(stringResource(R.string.intervention_history), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.history_limit_hint), style = MaterialTheme.typography.bodySmall)
        }
        if (state.events.isEmpty()) item { Text(stringResource(R.string.history_empty)) }
        items(state.events, key = { "event:" + it.id }) { event ->
            SectionCard {
                Text(event.appName, style = MaterialTheme.typography.titleMedium)
                Text(timestampText(event.occurredAt))
                Detail(stringResource(R.string.current_session), durationText(event.sessionDurationMillis))
                Detail(stringResource(R.string.session_limit), durationText(event.limitMillis))
                Detail(stringResource(R.string.incomplete_habits), event.incompleteHabitCount.toString())
            }
        }
    }
}
