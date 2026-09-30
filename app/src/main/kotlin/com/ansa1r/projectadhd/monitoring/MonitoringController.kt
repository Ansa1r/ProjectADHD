package com.ansa1r.projectadhd.monitoring

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class MonitoringController(
    private val context: Context,
    private val permissions: PermissionManager,
    private val monitoring: MonitoringState
) {
    fun start() {
        if (monitoring.state.value.status != MonitorStatus.STOPPED) return
        val access = permissions.state()
        if (!access.canMonitor) {
            monitoring.stopped(if (!access.usageAccess) MonitorIssue.USAGE_ACCESS else MonitorIssue.NOTIFICATIONS)
            return
        }
        monitoring.starting()
        try {
            ContextCompat.startForegroundService(context, Intent(context, UsageMonitoringService::class.java))
        } catch (_: RuntimeException) {
            monitoring.stopped(MonitorIssue.START_FAILED)
        }
    }

    fun stop() {
        context.stopService(Intent(context, UsageMonitoringService::class.java))
    }
}
