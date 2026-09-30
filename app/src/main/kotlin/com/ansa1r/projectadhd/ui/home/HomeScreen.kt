package com.ansa1r.projectadhd.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.monitoring.MonitorStatus
import com.ansa1r.projectadhd.ui.components.*
import com.ansa1r.projectadhd.util.durationText
import com.ansa1r.projectadhd.util.timestampText

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }
    RefreshOnResume(viewModel::refresh)
    ScreenList {
        item { MessageBanner(viewModel) }
        item {
            Text(stringResource(R.string.home_intro), style = MaterialTheme.typography.bodyLarge)
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        item {
            SectionCard {
                Detail(stringResource(R.string.usage_access), permissionText(state.permissions.usageAccess))
                Detail(stringResource(R.string.notification_permission), permissionText(state.permissions.notifications))
                Detail(stringResource(R.string.channels_status),
                    permissionText(state.permissions.monitoringChannel && state.permissions.interventionChannel))
                if (!state.permissions.usageAccess) {
                    Button(onClick = viewModel::usageSettings, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.grant_usage_access))
                    }
                }
                if (!state.permissions.notifications) {
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else viewModel.notificationSettings()
                    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.grant_notifications)) }
                }
                if (!state.permissions.notifications || !state.permissions.monitoringChannel || !state.permissions.interventionChannel) {
                    OutlinedButton(onClick = viewModel::notificationSettings, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.notification_settings))
                    }
                }
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
                } else {
                    OutlinedButton(onClick = viewModel::stop, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.stop_monitoring))
                    }
                }
                Detail(stringResource(R.string.foreground_app),
                    state.monitoring.foregroundName ?: stringResource(R.string.foreground_unknown))
                Detail(stringResource(R.string.current_session), durationText(state.monitoring.session?.durationMillis ?: 0))
                Detail(stringResource(R.string.incomplete_habits), state.incompleteHabits.toString())
                Detail(stringResource(R.string.last_intervention), timestampText(state.lastEventAt))
                Detail(stringResource(R.string.state_updated), timestampText(state.monitoring.updatedAt))
            }
        }
        item {
            OutlinedButton(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.refresh)) }
            Text(stringResource(R.string.home_monitoring_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}
