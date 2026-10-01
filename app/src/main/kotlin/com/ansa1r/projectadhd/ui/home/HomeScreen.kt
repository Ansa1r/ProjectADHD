package com.ansa1r.projectadhd.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.monitoring.MonitorStatus
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.ui.mascot.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    openHabits: () -> Unit = {},
    openApps: () -> Unit = {},
    openStats: () -> Unit = {},
    openPermissions: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refresh)
    ScreenList {
        item { MessageBanner(viewModel) }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MascotView(MascotMood.IDLE, Modifier.size(192.dp))
                Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            }
        }
        item {
            SectionCard {
                Text(stringResource(R.string.today_progress, state.tasks.completed, state.tasks.total), style = MaterialTheme.typography.titleLarge)
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                else LinearProgressIndicator(progress = { if (state.tasks.total == 0) 0f else state.tasks.completed.toFloat() / state.tasks.total }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer)
                Text(stringResource(if (state.tasks.total == 0) R.string.no_tasks_hint else if (state.tasks.incomplete == 0) R.string.praise_body else R.string.tasks_remaining, state.tasks.incomplete))
                BrandButton(onClick = openHabits, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.go_to_tasks)) }
            }
        }
        if (state.showAppSelection) item {
            MenuCard(stringResource(R.string.choose_apps), stringResource(R.string.home_apps_body),
                R.drawable.ic_nav_apps, openApps)
        }
        item {
            MenuCard(stringResource(R.string.home_stats_title), stringResource(R.string.home_stats_body),
                R.drawable.ic_nav_stats, openStats)
        }
        if (state.blocks.isNotEmpty()) item {
            SectionCard {
                Text(stringResource(R.string.active_blocks), style = MaterialTheme.typography.titleMedium)
                Text(state.blocks.joinToString { it.appName })
                Text(stringResource(R.string.block_instruction))
            }
        }
        item {
            SectionCard {
                Detail(stringResource(R.string.monitoring), monitorText(state.monitoring.status))
                MonitorIssueText(state.monitoring.issue)
                if (state.monitoring.status == MonitorStatus.STOPPED) {
                    BrandButton(onClick = viewModel::start, enabled = state.permissions.canMonitor && !state.loading,
                        modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_monitoring)) }
                    if (!state.permissions.canMonitor) Text(stringResource(R.string.home_start_permission_hint))
                } else OutlinedButton(onClick = viewModel::stop, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.stop_monitoring)) }
            }
        }
        if (!state.permissions.canMonitor || !state.permissions.overlay) item {
            SectionCard {
                Text(stringResource(R.string.home_permissions_hint), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = openPermissions) { Text(stringResource(R.string.open_permissions)) }
            }
        }
    }
}
