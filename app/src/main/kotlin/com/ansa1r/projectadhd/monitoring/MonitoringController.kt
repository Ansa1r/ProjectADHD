package com.ansa1r.projectadhd.monitoring

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ansa1r.projectadhd.data.repository.BlockRepository
import com.ansa1r.projectadhd.domain.model.ReleaseReason
import com.ansa1r.projectadhd.overlay.OverlayController
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MonitoringController(
    private val context: Context, private val permissions: PermissionManager,
    private val monitoring: MonitoringState, private val overlays: OverlayController,
    private val blocks: BlockRepository
) {
    val gate = Mutex()
    @Volatile var stopRequested = false
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stopJob: Job? = null
    fun start() {
        if (monitoring.state.value.status != MonitorStatus.STOPPED || stopJob?.isActive == true) return
        val access = permissions.state()
        if (!access.canMonitor) {
            monitoring.stopped(if (!access.usageAccess) MonitorIssue.USAGE_ACCESS else MonitorIssue.NOTIFICATIONS)
            return
        }
        stopRequested = false
        monitoring.starting()
        try {
            ContextCompat.startForegroundService(context, Intent(context, UsageMonitoringService::class.java))
        } catch (_: RuntimeException) { monitoring.stopped(MonitorIssue.START_FAILED) }
    }
    fun stop() {
        stopRequested = true
        overlays.cancelTest(); overlays.hide()
        if (stopJob?.isActive == true) return
        stopJob = scope.launch {
            var issue = MonitorIssue.NONE
            try {
                // Keep the existing FGS alive until the persistent stop transaction has committed.
                gate.withLock { blocks.clear(System.currentTimeMillis(), ReleaseReason.MONITORING_STOPPED) }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (_: Exception) { issue = MonitorIssue.DATA_ERROR }
            finally {
                monitoring.stopped(issue)
                context.stopService(Intent(context, UsageMonitoringService::class.java))
            }
        }
    }
}
