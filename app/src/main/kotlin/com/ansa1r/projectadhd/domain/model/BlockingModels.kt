package com.ansa1r.projectadhd.domain.model

enum class MascotMood { IDLE, BLOCKING, PRAISE }
enum class InterventionType { LEGACY_NOTIFICATION, BLOCK_TRIGGERED, BLOCK_RELEASED, PRAISE_SHOWN, FALLBACK_NOTIFICATION }
enum class ReleaseReason { COMPLETION, NO_TASKS, NEW_DAY, TASKS_CHANGED, TRACKING_DISABLED, MONITORING_STOPPED, DEBUG_CLEAR, CLOCK_CHANGED }

data class DailyTask(val id: Long, val completedAt: Long?)
data class DailyTaskSummary(val total: Int = 0, val completed: Int = 0) {
    init { require(total >= 0 && completed in 0..total) }
    val incomplete: Int get() = total - completed
}
data class DailyTaskSnapshot(val localDate: String, val tasks: List<DailyTask>) {
    val summary get() = DailyTaskSummary(tasks.size, tasks.count { it.completedAt != null })
    val incompleteIds get() = tasks.filter { it.completedAt == null }.map { it.id }.toSet()
}
data class BlockSession(
    val packageName: String,
    val appName: String,
    val startedAt: Long,
    val localDate: String,
    val triggerSessionDurationMillis: Long,
    val limitMillis: Long,
    val baselineCompletedCount: Int,
    val eligibleHabitIds: Set<Long>,
    val active: Boolean = true,
    val releasedAt: Long? = null,
    val releaseReason: ReleaseReason? = null
)
