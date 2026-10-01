package com.ansa1r.projectadhd.domain.model

import com.ansa1r.projectadhd.domain.habits.*

data class Habit(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val isActive: Boolean,
    val completedToday: Boolean,
    val targetDurationMinutes: Int = 30,
    val type: HabitType = HabitType.MANUAL,
    val linkedAppPackage: String? = null,
    val progress: HabitProgress = HabitProgress(),
    val legacyCompletion: Boolean = false
)

data class TrackedApp(
    val packageName: String,
    val displayName: String,
    val sessionLimitMinutes: Int = 15,
    val enabled: Boolean = true
) {
    init { require(sessionLimitMinutes in 1..180) }
    val limitMillis: Long get() = sessionLimitMinutes * 60_000L
}

data class InstalledApp(val packageName: String, val displayName: String)

data class InterventionEvent(
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val sessionDurationMillis: Long,
    val limitMillis: Long,
    val occurredAt: Long,
    val incompleteHabitCount: Int,
    val type: InterventionType = InterventionType.LEGACY_NOTIFICATION,
    val detail: String = ""
)

data class AppSettings(
    val cooldownMinutes: Int = 30,
    val lastInterventionAt: Long? = null,
    val lastMonitoringStartedAt: Long? = null,
    val praiseCooldownMinutes: Int = 30,
    val lastPraiseAt: Long? = null
)

data class RecordCounts(val habits: Int = 0, val completions: Int = 0, val apps: Int = 0, val events: Int = 0)
