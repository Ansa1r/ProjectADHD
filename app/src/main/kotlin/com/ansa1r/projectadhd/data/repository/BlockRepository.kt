package com.ansa1r.projectadhd.data.repository

import androidx.room.withTransaction
import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.local.entity.*
import com.ansa1r.projectadhd.domain.intervention.*
import com.ansa1r.projectadhd.domain.model.*
import com.ansa1r.projectadhd.util.dayKey
import kotlinx.coroutines.flow.map

class BlockRepository(private val database: AppDatabase) {
    private val dao get() = database.blocks()
    fun observeActive() = dao.observeActive().map { rows -> rows.map { it.domain() } }
    fun observeLastUnlock() = dao.observeLastUnlock()
    suspend fun find(packageName: String) = dao.find(packageName)?.domain()
    suspend fun today(now: Long): DailyTaskSnapshot {
        val date = dayKey(now)
        return DailyTaskSnapshot(date, database.habits().dailyTasks(date).map { DailyTask(it.id, it.completedAt) })
    }
    suspend fun start(payload: InterventionPayload, now: Long): BlockSession? = database.withTransaction {
        if (database.habits().linkedCount(payload.packageName) > 0) return@withTransaction null
        dao.find(payload.packageName)?.domain()?.takeIf { it.active }?.let { return@withTransaction it }
        if (database.trackedApps().find(payload.packageName)?.enabled != true) return@withTransaction null
        // Re-read atomically so a simultaneous completion cannot create a stale block.
        val today = today(now)
        if (today.summary.incomplete == 0) return@withTransaction null
        val block = BlockSession(payload.packageName, payload.appName, now, today.localDate,
            payload.sessionDurationMillis, payload.limitMillis, today.summary.completed, today.incompleteIds)
        dao.upsert(BlockSessionEntity.from(block))
        database.interventions().insert(event(block, now, InterventionType.BLOCK_TRIGGERED, today.summary.incomplete))
        block
    }
    suspend fun reconcile(now: Long, isTrackable: (String) -> Boolean = { true }) = database.withTransaction {
        val today = today(now)
        for (row in dao.active()) {
            val block = row.domain()
            val tracked = database.trackedApps().find(block.packageName)
            val reason = if (tracked?.enabled != true || !isTrackable(block.packageName) || database.habits().linkedCount(block.packageName) > 0) {
                ReleaseReason.TRACKING_DISABLED
            } else BlockCoordinator.releaseReason(block, today, now)
            if (reason != null) release(block, now, reason, today.summary.incomplete)
        }
    }
    /** Called inside the completion transaction; immediate undo cannot erase the transition. */
    suspend fun onNewCompletion(habitId: Long, date: String, at: Long) {
        val remaining = database.habits().incompleteCount(date)
        for (row in dao.active()) {
            val block = row.domain()
            if (BlockCoordinator.isNewCompletion(block, habitId, date, at)) {
                release(block, at, ReleaseReason.COMPLETION, remaining)
            }
        }
    }
    suspend fun clear(now: Long, reason: ReleaseReason) = database.withTransaction {
        val remaining = today(now).summary.incomplete
        dao.active().forEach { release(it.domain(), now, reason, remaining) }
    }
    private suspend fun release(block: BlockSession, now: Long, reason: ReleaseReason, remaining: Int) {
        if (dao.release(block.packageName, now, reason.name) == 1) {
            database.interventions().insert(event(block, now, InterventionType.BLOCK_RELEASED, remaining, reason.name))
        }
    }
    private fun event(block: BlockSession, now: Long, type: InterventionType, remaining: Int, detail: String = "") =
        InterventionEventEntity(packageName = block.packageName, appName = block.appName,
            sessionDurationMillis = block.triggerSessionDurationMillis, limitMillis = block.limitMillis,
            occurredAt = now, incompleteHabitCount = remaining, type = type.name, detail = detail)
}
