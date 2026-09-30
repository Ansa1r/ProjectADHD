package com.ansa1r.projectadhd.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ansa1r.projectadhd.domain.model.BlockSession
import com.ansa1r.projectadhd.domain.model.ReleaseReason

/** One current/latest row per package makes duplicate active sessions impossible. */
@Entity(tableName = "block_sessions")
data class BlockSessionEntity(
    @PrimaryKey val packageName: String,
    val appName: String, val startedAt: Long, val localDate: String,
    val triggerSessionDurationMillis: Long, val limitMillis: Long,
    val baselineCompletedCount: Int, val eligibleHabitIds: String,
    val active: Boolean, val releasedAt: Long?, val releaseReason: String?
) {
    fun domain() = BlockSession(packageName, appName, startedAt, localDate,
        triggerSessionDurationMillis, limitMillis, baselineCompletedCount,
        eligibleHabitIds.split(',').mapNotNull(String::toLongOrNull).toSet(), active,
        releasedAt, releaseReason?.let(ReleaseReason::valueOf))
    companion object {
        fun from(block: BlockSession) = with(block) {
            BlockSessionEntity(packageName, appName, startedAt, localDate,
                triggerSessionDurationMillis, limitMillis, baselineCompletedCount,
                eligibleHabitIds.sorted().joinToString(","), active, releasedAt, releaseReason?.name)
        }
    }
}
