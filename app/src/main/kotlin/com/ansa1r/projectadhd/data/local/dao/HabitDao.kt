package com.ansa1r.projectadhd.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ansa1r.projectadhd.data.local.entity.HabitCompletionEntity
import com.ansa1r.projectadhd.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

data class HabitRow(@Embedded val habit: HabitEntity, val completedToday: Boolean)

data class DailyTaskRow(val id: Long, val completedAt: Long?)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun find(id: Long): HabitEntity?
    @Query("SELECT * FROM habits WHERE isActive = 1")
    suspend fun active(): List<HabitEntity>
    @Query("SELECT linkedAppPackage FROM habits WHERE isActive = 1 AND type = 'APP_BASED' AND linkedAppPackage IS NOT NULL")
    fun observeLinkedPackages(): Flow<List<String>>
    @Query("SELECT COUNT(*) FROM habits WHERE isActive = 1 AND type = 'APP_BASED' AND linkedAppPackage = :pkg")
    suspend fun linkedCount(pkg: String): Int
    @Query("UPDATE habits SET activatedAt = :at WHERE id = :id")
    suspend fun activateAt(id: Long, at: Long)

    @Query("SELECT h.id, c.completedAt FROM habits h LEFT JOIN habit_completions c ON h.id = c.habitId AND c.localDate = :date WHERE h.isActive = 1 AND h.createdAt < :until AND h.activatedAt < :until")
    suspend fun dailyTasksBefore(date: String, until: Long): List<DailyTaskRow>

    @Query("SELECT h.id, c.completedAt FROM habits h LEFT JOIN habit_completions c ON h.id = c.habitId AND c.localDate = :date WHERE h.isActive = 1")
    suspend fun dailyTasks(date: String): List<DailyTaskRow>

    @Query("""
        SELECT habits.*, EXISTS(
            SELECT 1 FROM habit_completions c WHERE c.habitId = habits.id AND c.localDate = :date
        ) AS completedToday FROM habits ORDER BY isActive DESC, createdAt DESC, id DESC
    """)
    fun observeDay(date: String): Flow<List<HabitRow>>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Query("UPDATE habits SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE habits SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun complete(completion: HabitCompletionEntity): Long

    @Query("DELETE FROM habit_completions WHERE habitId = :id AND localDate = :date")
    suspend fun undoCompletion(id: Long, date: String)

    @Query("""
        SELECT COUNT(*) FROM habits h WHERE h.isActive = 1 AND NOT EXISTS(
            SELECT 1 FROM habit_completions c WHERE c.habitId = h.id AND c.localDate = :date
        )
    """)
    suspend fun incompleteCount(date: String): Int

    @Query("SELECT COUNT(*) FROM habits")
    fun observeHabitCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM habit_completions")
    fun observeCompletionCount(): Flow<Int>
}
