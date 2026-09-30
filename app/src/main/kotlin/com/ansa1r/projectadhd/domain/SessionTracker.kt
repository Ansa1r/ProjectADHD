package com.ansa1r.projectadhd.domain

import com.ansa1r.projectadhd.domain.model.AppSession
import com.ansa1r.projectadhd.domain.model.UsageSignal
import com.ansa1r.projectadhd.domain.model.UsageSignalType

class SessionTracker(private val activityTransitionGraceMillis: Long = 1_000) {
    private var packageName: String? = null
    private var startedAt = 0L
    private var lastEventAt = Long.MIN_VALUE
    private var pausedAt: Long? = null
    private val resumedActivities = mutableSetOf<String>()

    fun accept(event: UsageSignal) {
        if (event.timestamp < lastEventAt) return
        lastEventAt = event.timestamp
        expirePause(event.timestamp)
        when (event.type) {
            UsageSignalType.RESUMED -> {
                val pkg = event.packageName ?: return
                if (packageName != pkg) {
                    clearSession()
                    packageName = pkg
                    startedAt = event.timestamp
                }
                resumedActivities.add(event.activityKey)
                pausedAt = null
            }
            UsageSignalType.PAUSED -> {
                if (packageName == event.packageName && resumedActivities.remove(event.activityKey) &&
                    resumedActivities.isEmpty()) pausedAt = event.timestamp
            }
            // Public UsageEvents has no instance ID. A late stop can belong to an older instance.
            UsageSignalType.STOPPED -> Unit
            UsageSignalType.SCREEN_OFF, UsageSignalType.LOCKED, UsageSignalType.SHUTDOWN -> clearSession()
        }
    }

    fun snapshot(nowMillis: Long): AppSession? {
        expirePause(nowMillis)
        val pkg = packageName ?: return null
        if (pausedAt != null || nowMillis < startedAt) return null
        return AppSession(pkg, startedAt, nowMillis - startedAt)
    }

    fun reset() {
        clearSession()
        lastEventAt = Long.MIN_VALUE
    }

    private fun expirePause(now: Long) {
        val paused = pausedAt ?: return
        if (now - paused > activityTransitionGraceMillis) clearSession()
    }

    private fun clearSession() {
        packageName = null
        startedAt = 0
        pausedAt = null
        resumedActivities.clear()
    }
}
