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
import com.ansa1r.projectadhd.domain.intervention.*
import com.ansa1r.projectadhd.domain.model.*
import com.ansa1r.projectadhd.overlay.OverlayResult
import kotlinx.coroutines.sync.withLock
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
            container.controller.stop()
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
        container.excludedApps.refresh()
        loop = scope.launch {
            try {
                container.preferences.markMonitoringStarted(System.currentTimeMillis())
                while (isActive) {
                    val issue = container.controller.gate.withLock {
                        if (container.controller.stopRequested) return@withLock MonitorIssue.NONE
                        poll()
                    }
                    if (issue != MonitorIssue.NONE) {
                        stopIssue = issue
                        stopSelf()
                        break
                    }
                    delay(1_000)
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
            container.overlays.foregroundChanged(null)
            container.blocks.reconcile(now) { !container.excludedApps.contains(it) }
            container.monitoring.set(MonitoringSnapshot(status = MonitorStatus.RUNNING, updatedAt = now,
                tasks = container.blocks.today(now).summary))
            return MonitorIssue.NONE
        }
        val from = if (cursor == 0L) now - 86_400_000L else cursor - 10_000
        // A short overlap collects late Android events; already processed events are ignored.
        val events = try { withContext(Dispatchers.IO) { container.usage.events(from.coerceAtLeast(sessionFloor), now) } }
            catch (_: SecurityException) { return MonitorIssue.USAGE_ACCESS }
        events.sortedBy { it.timestamp }.filter { seen.add(it) }.forEach(tracker::accept)
        seen.removeAll { it.timestamp < now - 15_000 }
        cursor = now
        val rawSession = tracker.snapshot(now)
        container.overlays.foregroundChanged(rawSession?.packageName)
        container.blocks.reconcile(now) { !container.excludedApps.contains(it) }
        val savedBlock = rawSession?.let { container.blocks.find(it.packageName) }
        val session = SessionAllowance.apply(rawSession, savedBlock?.releasedAt, now)
        val settings = container.preferences.settings.first()
        container.overlays.setBlockingOpacity(settings.blockingOverlayOpacityPercent)
        val app = session?.let { container.trackedApps.find(it.packageName) }
        val tasks = container.blocks.today(now).summary
        val decision = container.engine.decide(InterventionInput(
            foregroundPackage = session?.packageName, trackedApp = app,
            sessionDurationMillis = session?.durationMillis ?: 0, nowMillis = now,
            tasks = tasks, activeBlock = savedBlock?.takeIf { it.active },
            lastPraiseMillis = settings.lastPraiseAt,
            praiseCooldownMillis = settings.praiseCooldownMinutes * 60_000L,
            excluded = session?.let { container.excludedApps.contains(it.packageName) } == true,
            monitoringEnabled = !container.controller.stopRequested
        ))
        if (container.controller.stopRequested) return MonitorIssue.NONE
        container.monitoring.set(MonitoringSnapshot(
            status = MonitorStatus.RUNNING, session = session, updatedAt = now, tasks = tasks,
            limitMillis = app?.limitMillis,
            foregroundName = session?.let { app?.displayName ?: container.installedApps.nameOf(it.packageName) },
            decision = decision
        ))
        when (decision) {
            is InterventionDecision.Block -> {
                val block = container.blocks.start(decision.payload, now)
                if (block != null && !container.controller.stopRequested) {
                    val payload = decision.payload.copy(sessionDurationMillis = block.triggerSessionDurationMillis)
                    val result = container.overlays.showBlocking(payload)
                    if (result is OverlayResult.Failed) return fallback(payload, result.reason, now, settings, false)
                } else container.overlays.hideProductionBlock()
            }
            is InterventionDecision.Praise -> {
                // Durable attempt reservation prevents repeated praise/fallback across process death.
                container.preferences.markPraise(now)
                if (container.controller.stopRequested) return MonitorIssue.NONE
                when (val result = container.overlays.showPraise(decision.payload)) {
                    is OverlayResult.Failed -> return fallback(decision.payload, result.reason, now, settings, true)
                    OverlayResult.Shown -> record(decision.payload, now, InterventionType.PRAISE_SHOWN)
                    OverlayResult.Suppressed -> Unit
                }
            }
            is InterventionDecision.None -> container.overlays.hideProductionBlock()
        }
        // Debug display requests do not create sessions, events or change production cooldowns.
        if (decision !is InterventionDecision.Block && decision !is InterventionDecision.Praise) {
            val test = container.overlays.takeTest(session?.packageName, app?.enabled == true)
            if (test != null && session != null && app != null) {
                val payload = InterventionPayload(app.packageName, app.displayName,
                    app.limitMillis, app.limitMillis, maxOf(1, tasks.incomplete))
                if (test == MascotMood.BLOCKING) container.overlays.showBlocking(payload, test = true)
                else container.overlays.showPraise(payload, test = true)
            }
        }
        return MonitorIssue.NONE
    }

    private suspend fun fallback(payload: InterventionPayload, reason: String, now: Long,
        settings: AppSettings, praise: Boolean): MonitorIssue {
        if (!Cooldown.expired(now, settings.lastInterventionAt, settings.cooldownMinutes * 60_000L)) {
            return MonitorIssue.NONE
        }
        container.preferences.markIntervention(now)
        if (container.controller.stopRequested) return MonitorIssue.NONE
        val sent = container.notifications.intervention(payload, praise)
        record(payload, now, InterventionType.FALLBACK_NOTIFICATION,
            (if (praise) "PRAISE: " else "BLOCK: ") + reason + if (sent) "" else "; NOTIFICATION_REJECTED")
        return if (sent) MonitorIssue.NONE else MonitorIssue.NOTIFICATIONS
    }

    private suspend fun record(payload: InterventionPayload, now: Long, type: InterventionType, detail: String = "") {
        container.interventions.add(InterventionEvent(
            packageName = payload.packageName, appName = payload.appName,
            sessionDurationMillis = payload.sessionDurationMillis, limitMillis = payload.limitMillis,
            occurredAt = now, incompleteHabitCount = payload.incompleteHabitCount, type = type, detail = detail))
    }

    override fun onDestroy() {
        scope.cancel()
        container.overlays.cancelTest()
        container.overlays.hide()
        tracker.reset()
        if (!container.controller.stopRequested) container.monitoring.stopped(stopIssue)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
