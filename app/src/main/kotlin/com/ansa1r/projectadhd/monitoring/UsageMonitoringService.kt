package com.ansa1r.projectadhd.monitoring

import android.app.KeyguardManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import com.ansa1r.projectadhd.ProjectADHDApplication
import com.ansa1r.projectadhd.domain.SessionTracker
import com.ansa1r.projectadhd.domain.intervention.InterventionDecision
import com.ansa1r.projectadhd.domain.intervention.InterventionInput
import com.ansa1r.projectadhd.domain.model.InterventionEvent
import com.ansa1r.projectadhd.domain.model.UsageSignal
import com.ansa1r.projectadhd.notification.NotificationHelper
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsageMonitoringService : Service() {
    companion object { const val ACTION_STOP = "com.ansa1r.projectadhd.STOP_MONITORING" }

    private val container get() = (application as ProjectADHDApplication).container
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var loop: Job? = null
    private val tracker = SessionTracker()
    private val seen = mutableSetOf<UsageSignal>()
    private var cursor = 0L
    private var sessionFloor = 0L
    private var lastWall = 0L
    private var lastElapsed = 0L
    private var stopIssue = MonitorIssue.NONE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (loop?.isActive == true) return START_NOT_STICKY
        try {
            val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            ServiceCompat.startForeground(this, NotificationHelper.MONITORING_ID,
                container.notifications.monitoringNotification(), type)
        } catch (_: RuntimeException) {
            stopIssue = MonitorIssue.START_FAILED
            stopSelf()
            return START_NOT_STICKY
        }
        loop = scope.launch {
            try {
                container.preferences.markMonitoringStarted(System.currentTimeMillis())
                while (isActive) {
                    val issue = withContext(Dispatchers.IO) { poll() }
                    if (issue != MonitorIssue.NONE) {
                        stopIssue = issue
                        stopSelf()
                        break
                    }
                    delay(5_000)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                stopIssue = MonitorIssue.DATA_ERROR
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun poll(): MonitorIssue {
        val permissions = container.permissions.state()
        if (!permissions.usageAccess) return MonitorIssue.USAGE_ACCESS
        if (!permissions.canMonitor) return MonitorIssue.NOTIFICATIONS
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        if (lastWall != 0L && abs((now - lastWall) - (elapsed - lastElapsed)) > 5_000) {
            tracker.reset()
            seen.clear()
            cursor = now
            sessionFloor = now
        }
        lastWall = now
        lastElapsed = elapsed
        val deviceActive = getSystemService(PowerManager::class.java)?.isInteractive == true &&
            getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == false
        if (!deviceActive) {
            tracker.reset()
            seen.clear()
            cursor = now
            sessionFloor = now
            container.monitoring.set(MonitoringSnapshot(status = MonitorStatus.RUNNING, updatedAt = now))
            return MonitorIssue.NONE
        }
        val from = if (cursor == 0L) now - 86_400_000L else cursor - 10_000
        // A short overlap collects late Android events; already processed events are ignored.
        val events = try { container.usage.events(from.coerceAtLeast(sessionFloor), now) }
            catch (_: SecurityException) { return MonitorIssue.USAGE_ACCESS }
        events.sortedBy { it.timestamp }.filter { seen.add(it) }.forEach(tracker::accept)
        seen.removeAll { it.timestamp < now - 15_000 }
        cursor = now
        val session = tracker.snapshot(now)
        val settings = container.preferences.settings.first()
        val app = session?.let { container.trackedApps.find(it.packageName) }
        val decision = container.engine.decide(InterventionInput(
            foregroundPackage = session?.packageName, trackedApp = app,
            sessionDurationMillis = session?.durationMillis ?: 0, nowMillis = now,
            lastInterventionMillis = settings.lastInterventionAt,
            cooldownMillis = settings.cooldownMinutes * 60_000L,
            incompleteHabitCount = container.habits.incompleteCount(now)
        ))
        container.monitoring.set(MonitoringSnapshot(
            status = MonitorStatus.RUNNING, session = session, updatedAt = now,
            foregroundName = session?.let { app?.displayName ?: container.installedApps.nameOf(it.packageName) },
            decision = decision
        ))
        if (decision is InterventionDecision.Notify) {
            val latestPermissions = container.permissions.state()
            if (!latestPermissions.usageAccess) return MonitorIssue.USAGE_ACCESS
            if (!latestPermissions.canMonitor) return MonitorIssue.NOTIFICATIONS
            // Reserve cooldown before the side effect, so a process death cannot cause notification spam.
            container.preferences.markIntervention(now)
            if (!container.notifications.intervention(decision)) return MonitorIssue.NOTIFICATIONS
            container.interventions.add(InterventionEvent(
                packageName = decision.packageName, appName = decision.appName,
                sessionDurationMillis = decision.sessionDurationMillis, limitMillis = decision.limitMillis,
                occurredAt = now, incompleteHabitCount = decision.incompleteHabitCount
            ))
        }
        return MonitorIssue.NONE
    }

    override fun onDestroy() {
        scope.cancel()
        tracker.reset()
        container.monitoring.stopped(stopIssue)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
