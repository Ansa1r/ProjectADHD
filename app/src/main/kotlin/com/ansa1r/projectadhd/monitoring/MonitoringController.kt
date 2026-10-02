package com.ansa1r.projectadhd.monitoring

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ansa1r.projectadhd.data.preferences.AppPreferences
import com.ansa1r.projectadhd.data.repository.BlockRepository
import com.ansa1r.projectadhd.domain.model.ReleaseReason
import com.ansa1r.projectadhd.overlay.OverlayController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MonitoringController(
    private val context: Context, private val permissions: PermissionManager,
    private val monitoring: MonitoringState, private val overlays: OverlayController,
    private val blocks: BlockRepository, private val preferences: AppPreferences
) {
    val gate = Mutex()
    private val commands = Mutex()
    @Volatile var stopRequested = false
        private set
    @Volatile var serviceRunning = false
        private set
    private var destroyed = CompletableDeferred<Unit>().apply { complete(Unit) }

    fun serviceCreated() { serviceRunning = true; destroyed = CompletableDeferred() }
    fun serviceDestroyed(issue: MonitorIssue) {
        serviceRunning = false
        monitoring.stopped(issue)
        destroyed.complete(Unit)
    }

    /** Foreground repair never changes the user's preference. No public unconditional start. */
    suspend fun ensureIfEnabled() = commands.withLock {
        if (preferences.onboarding.first().shouldEnsureMonitoring) startLocked()
    }

    suspend fun enable() = commands.withLock {
        if (!preferences.onboarding.first().onboardingCompleted) return@withLock
        preferences.setMonitoringEnabled(true)
        startLocked()
    }

    private fun startLocked() {
        if (monitoring.state.value.status != MonitorStatus.STOPPED || serviceRunning) return
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

    suspend fun stop() = commands.withLock {
        // Commit the durable choice before tearing down the service. A failed write leaves it running.
        preferences.setMonitoringEnabled(false)
        stopRequested = true
        overlays.cancelTest(); overlays.hide()
        var issue = MonitorIssue.NONE
        try { gate.withLock { blocks.clear(System.currentTimeMillis(), ReleaseReason.MONITORING_STOPPED) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { issue = MonitorIssue.DATA_ERROR }
        finally {
            context.stopService(Intent(context, UsageMonitoringService::class.java))
            if (serviceRunning) withTimeoutOrNull(5_000) { destroyed.await() }
            monitoring.stopped(issue)
        }
    }
}
