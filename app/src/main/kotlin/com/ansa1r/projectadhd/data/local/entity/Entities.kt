package com.ansa1r.projectadhd.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val isActive: Boolean = true
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "localDate"],
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class HabitCompletionEntity(val habitId: Long, val localDate: String, val completedAt: Long)

@Entity(tableName = "tracked_apps")
data class TrackedAppEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val sessionLimitMinutes: Int = 15,
    val enabled: Boolean = true
)

@Entity(tableName = "intervention_events", indices = [Index("occurredAt")])
data class InterventionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val sessionDurationMillis: Long,
    val limitMillis: Long,
    val occurredAt: Long,
    val incompleteHabitCount: Int
)
