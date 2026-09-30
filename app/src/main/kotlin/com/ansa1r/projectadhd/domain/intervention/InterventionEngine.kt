package com.ansa1r.projectadhd.domain.intervention

import com.ansa1r.projectadhd.domain.model.TrackedApp

enum class NoInterventionReason { NO_FOREGROUND, NOT_TRACKED, BELOW_LIMIT, COOLDOWN }

sealed interface InterventionDecision {
    data class None(val reason: NoInterventionReason) : InterventionDecision
    data class Notify(
        val packageName: String,
        val appName: String,
        val sessionDurationMillis: Long,
        val limitMillis: Long,
        val incompleteHabitCount: Int
    ) : InterventionDecision
}

data class InterventionInput(
    val foregroundPackage: String?,
    val trackedApp: TrackedApp?,
    val sessionDurationMillis: Long,
    val nowMillis: Long,
    val lastInterventionMillis: Long?,
    val cooldownMillis: Long = 30 * 60_000L,
    val incompleteHabitCount: Int = 0
)

class InterventionEngine {
    fun decide(input: InterventionInput): InterventionDecision {
        val foreground = input.foregroundPackage
            ?: return InterventionDecision.None(NoInterventionReason.NO_FOREGROUND)
        val app = input.trackedApp
        if (app == null || !app.enabled || app.packageName != foreground) {
            return InterventionDecision.None(NoInterventionReason.NOT_TRACKED)
        }
        if (input.sessionDurationMillis < app.limitMillis) {
            return InterventionDecision.None(NoInterventionReason.BELOW_LIMIT)
        }
        val last = input.lastInterventionMillis
        if (last != null && (input.nowMillis < last ||
                input.nowMillis - last < input.cooldownMillis.coerceAtLeast(0))) {
            return InterventionDecision.None(NoInterventionReason.COOLDOWN)
        }
        return InterventionDecision.Notify(
            foreground, app.displayName, input.sessionDurationMillis,
            app.limitMillis, input.incompleteHabitCount.coerceAtLeast(0)
        )
    }
}
