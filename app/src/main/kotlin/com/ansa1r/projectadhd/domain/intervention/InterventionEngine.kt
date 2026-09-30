package com.ansa1r.projectadhd.domain.intervention

import com.ansa1r.projectadhd.domain.model.*

enum class NoInterventionReason { NO_FOREGROUND, NOT_TRACKED, BELOW_LIMIT, COOLDOWN, NO_TASKS, MONITORING_OFF, EXCLUDED }
data class InterventionPayload(
    val packageName: String, val appName: String, val sessionDurationMillis: Long,
    val limitMillis: Long, val incompleteHabitCount: Int
)
sealed interface InterventionDecision {
    data class None(val reason: NoInterventionReason) : InterventionDecision
    data class Block(val payload: InterventionPayload) : InterventionDecision
    data class Praise(val payload: InterventionPayload) : InterventionDecision
}
data class InterventionInput(
    val foregroundPackage: String?, val trackedApp: TrackedApp?,
    val sessionDurationMillis: Long, val nowMillis: Long, val tasks: DailyTaskSummary,
    val activeBlock: BlockSession? = null, val lastPraiseMillis: Long? = null,
    val praiseCooldownMillis: Long = 30 * 60_000L,
    val monitoringEnabled: Boolean = true, val excluded: Boolean = false
)
class InterventionEngine {
    fun decide(input: InterventionInput): InterventionDecision {
        fun none(reason: NoInterventionReason) = InterventionDecision.None(reason)
        if (!input.monitoringEnabled) return none(NoInterventionReason.MONITORING_OFF)
        val foreground = input.foregroundPackage ?: return none(NoInterventionReason.NO_FOREGROUND)
        if (input.excluded) return none(NoInterventionReason.EXCLUDED)
        val app = input.trackedApp
        if (app == null || !app.enabled || app.packageName != foreground) return none(NoInterventionReason.NOT_TRACKED)
        if (input.tasks.total == 0) return none(NoInterventionReason.NO_TASKS)
        val payload = InterventionPayload(foreground, app.displayName,
            input.sessionDurationMillis.coerceAtLeast(0), app.limitMillis, input.tasks.incomplete)
        if (input.activeBlock?.let { it.active && it.packageName == foreground } == true &&
            input.tasks.incomplete > 0) return InterventionDecision.Block(payload)
        if (input.sessionDurationMillis < app.limitMillis) return none(NoInterventionReason.BELOW_LIMIT)
        if (input.tasks.incomplete > 0) return InterventionDecision.Block(payload)
        if (!Cooldown.expired(input.nowMillis, input.lastPraiseMillis, input.praiseCooldownMillis)) {
            return none(NoInterventionReason.COOLDOWN)
        }
        return InterventionDecision.Praise(payload)
    }
}
object Cooldown {
    fun expired(now: Long, last: Long?, duration: Long): Boolean =
        last == null || (now >= last && now - last >= duration.coerceAtLeast(0))
}
