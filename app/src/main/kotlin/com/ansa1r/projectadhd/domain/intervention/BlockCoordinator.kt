package com.ansa1r.projectadhd.domain.intervention

import com.ansa1r.projectadhd.domain.model.*

/** Baseline IDs prevent recycling habits completed before the block as an unlock. */
object BlockCoordinator {
    fun releaseReason(block: BlockSession, today: DailyTaskSnapshot, now: Long): ReleaseReason? {
        if (!block.active) return null
        if (today.localDate != block.localDate) return ReleaseReason.NEW_DAY
        if (now < block.startedAt) return ReleaseReason.CLOCK_CHANGED
        if (today.tasks.any { isNewCompletion(block, it.id, today.localDate, it.completedAt) }) {
            return ReleaseReason.COMPLETION
        }
        if (today.tasks.isEmpty()) return ReleaseReason.NO_TASKS
        if (today.tasks.none { it.id in block.eligibleHabitIds }) {
            return ReleaseReason.TASKS_CHANGED
        }
        return null
    }
    fun isNewCompletion(block: BlockSession, habitId: Long, date: String, at: Long?): Boolean =
        block.active && block.localDate == date && habitId in block.eligibleHabitIds &&
            at != null && at > block.startedAt
}

/** A durable release timestamp clips old UsageEvents sessions after a process restart too. */
object SessionAllowance {
    fun apply(session: AppSession?, releasedAt: Long?, now: Long): AppSession? {
        session ?: return null
        val start = maxOf(session.startedAt, releasedAt?.takeIf { it <= now } ?: session.startedAt)
        return session.copy(startedAt = start, durationMillis = (now - start).coerceAtLeast(0))
    }
}
