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

@Dao
interface HabitDao {
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
    suspend fun complete(completion: HabitCompletionEntity)

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
