package com.ansa1r.projectadhd.data.local.entity

import androidx.room.*
import com.ansa1r.projectadhd.domain.habits.*

@Entity(tableName = "habit_daily_progress", primaryKeys = ["habitId", "localDate"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)])
data class HabitDailyEntity(
    val habitId: Long,
    val localDate: String,
    val accumulatedMillis: Long = 0,
    val extraTargetMinutes: Long = 0,
    val state: String = "READY",
    val checkpointElapsed: Long? = null,
    val bootCount: Int = -1,
    val sessionMillis: Long = 0,
    val checkpointWall: Long = 0,
    val appWindowBaseMillis: Long = 0
) {
    fun progress() = HabitProgress(accumulatedMillis, extraTargetMinutes, HabitState.valueOf(state), checkpointElapsed, bootCount, sessionMillis)
    fun withProgress(value: HabitProgress, wall: Long) = copy(accumulatedMillis = value.accumulatedMillis,
        extraTargetMinutes = value.extraTargetMinutes, state = value.state.name, checkpointElapsed = value.checkpointElapsed,
        bootCount = value.bootCount, sessionMillis = value.sessionMillis, checkpointWall = wall)
}

@Entity(tableName = "mascot_progress")
data class MascotEntity(
    @PrimaryKey val id: Int = 1,
    val totalXp: Long = 0,
    val completedHabits: Long = 0,
    val streak: Int = 0,
    val lastStreakRewardDate: String? = null,
    val evaluatedDate: String? = null
)

/** Immutable ledger, deliberately not cascaded when a habit is deleted. Key enforces exactly-once. */
@Entity(tableName = "xp_awards", indices = [Index("awardedAt")])
data class XpAwardEntity(
    @PrimaryKey val eventKey: String,
    val kind: String,
    val habitId: Long?,
    val localDate: String,
    val baseXp: Int,
    val awardedXp: Long,
    val levelBefore: Int,
    val awardedAt: Long
)

@Entity(tableName = "habit_days")
data class HabitDayEntity(
    @PrimaryKey val localDate: String,
    val activeCount: Int,
    val completedCount: Int,
    val streakAwarded: Boolean = false
)
