package com.ansa1r.projectadhd.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansa1r.projectadhd.R
import com.ansa1r.projectadhd.domain.intervention.InterventionDecision
import com.ansa1r.projectadhd.domain.intervention.NoInterventionReason
import com.ansa1r.projectadhd.monitoring.MonitorIssue
import com.ansa1r.projectadhd.monitoring.MonitorStatus
import com.ansa1r.projectadhd.ui.theme.BrandColors

@Composable
fun ScreenList(content: LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val calm = LocalCalmSurfaces.current
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (calm) BrandColors.Surface else BrandColors.PurpleSurface.copy(alpha = 0.85f),
            contentColor = BrandColors.Text),
        border = BorderStroke(if (calm) 1.dp else 2.dp, BrandColors.PurpleOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Detail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun MessageBanner(viewModel: AppViewModel) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    message?.let {
        SectionCard {
            Text(stringResource(it))
            TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.dismiss)) }
        }
    }
}

@Composable
fun RefreshOnResume(refresh: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val current by rememberUpdatedState(refresh)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) current()
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) current()
        onDispose { lifecycle.removeObserver(observer) }
    }
}

@Composable
fun permissionText(allowed: Boolean): String =
    stringResource(if (allowed) R.string.allowed else R.string.not_allowed)

@Composable
fun monitorText(status: MonitorStatus): String = stringResource(when (status) {
    MonitorStatus.STOPPED -> R.string.monitor_stopped
    MonitorStatus.STARTING -> R.string.monitor_starting
    MonitorStatus.RUNNING -> R.string.monitor_running
})

@Composable
fun MonitorIssueText(issue: MonitorIssue) {
    val resource = when (issue) {
        MonitorIssue.NONE -> return
        MonitorIssue.USAGE_ACCESS -> R.string.usage_required
        MonitorIssue.NOTIFICATIONS -> R.string.notifications_required
        MonitorIssue.START_FAILED -> R.string.error_start_service
        MonitorIssue.DATA_ERROR -> R.string.error_monitor_data
    }
    Text(stringResource(resource), color = MaterialTheme.colorScheme.error)
}

@Composable
fun decisionText(decision: InterventionDecision): String = when (decision) {
    is InterventionDecision.Block -> stringResource(R.string.decision_block, decision.payload.appName)
    is InterventionDecision.Praise -> stringResource(R.string.decision_praise, decision.payload.appName)
    is InterventionDecision.None -> stringResource(when (decision.reason) {
        NoInterventionReason.NO_FOREGROUND -> R.string.decision_no_foreground
        NoInterventionReason.NOT_TRACKED -> R.string.decision_not_tracked
        NoInterventionReason.BELOW_LIMIT -> R.string.decision_below_limit
        NoInterventionReason.COOLDOWN -> R.string.decision_cooldown
        NoInterventionReason.NO_TASKS -> R.string.no_tasks_hint
        NoInterventionReason.MONITORING_OFF -> R.string.monitor_stopped
        NoInterventionReason.EXCLUDED -> R.string.decision_excluded
    })
}
