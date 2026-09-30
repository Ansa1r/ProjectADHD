package com.ansa1r.projectadhd.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.model.MascotMood
import com.ansa1r.projectadhd.monitoring.MonitorStatus
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.ui.mascot.*

@Composable
fun HomeScreen(viewModel: HomeViewModel, openHabits: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }
    RefreshOnResume(viewModel::refresh)
    MascotBackdrop(Modifier.fillMaxSize()) {
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
                    else LinearProgressIndicator(progress = { if (state.tasks.total == 0) 0f else state.tasks.completed.toFloat() / state.tasks.total }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(if (state.tasks.total == 0) R.string.no_tasks_hint else if (state.tasks.incomplete == 0) R.string.praise_body else R.string.tasks_remaining, state.tasks.incomplete))
                    Button(onClick = openHabits, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.go_to_tasks)) }
                }
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
                        Button(onClick = viewModel::start, enabled = state.permissions.canMonitor && !state.loading,
                            modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_monitoring)) }
                        if (!state.permissions.canMonitor) Text(stringResource(R.string.monitoring_requirements))
                    } else OutlinedButton(onClick = viewModel::stop, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.stop_monitoring)) }
                    if (!state.permissions.overlay || !state.permissions.overlaySupported) Text(stringResource(R.string.overlay_fallback_hint))
                }
            }
            item {
                SectionCard {
                    Text(stringResource(R.string.permissions_title), style = MaterialTheme.typography.titleMedium)
                    Detail(stringResource(R.string.usage_access), permissionText(state.permissions.usageAccess))
                    if (!state.permissions.usageAccess) OutlinedButton(onClick = viewModel::usageSettings) { Text(stringResource(R.string.grant_usage_access)) }
                    Detail(stringResource(R.string.overlay_permission), permissionText(state.permissions.overlay && state.permissions.overlaySupported))
                    if (state.permissions.overlaySupported && !state.permissions.overlay) OutlinedButton(onClick = viewModel::overlaySettings) { Text(stringResource(R.string.grant_overlay)) }
                    Detail(stringResource(R.string.notification_permission), permissionText(state.permissions.notifications))
                    if (!state.permissions.notifications) OutlinedButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                            Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else viewModel.notificationSettings()
                    }) { Text(stringResource(R.string.grant_notifications)) }
                    if (!state.permissions.monitoringChannel || !state.permissions.interventionChannel) {
                        OutlinedButton(onClick = viewModel::notificationSettings) { Text(stringResource(R.string.notification_settings)) }
                    }
                }
            }
        }
    }
}
