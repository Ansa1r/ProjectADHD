package com.ansa1r.projectadhd.data.local.dao

import androidx.room.*
import com.ansa1r.projectadhd.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitProgressDao {
    @Query("SELECT * FROM habit_daily_progress WHERE localDate = :date")
    fun observeDay(date: String): Flow<List<HabitDailyEntity>>
    @Query("SELECT * FROM habit_daily_progress WHERE habitId = :id AND localDate = :date")
    suspend fun find(id: Long, date: String): HabitDailyEntity?
    @Query("SELECT * FROM habit_daily_progress WHERE state = 'IN_PROGRESS'")
    suspend fun running(): List<HabitDailyEntity>
    @Upsert suspend fun save(progress: HabitDailyEntity)
    @Query("SELECT * FROM mascot_progress WHERE id = 1")
    suspend fun mascot(): MascotEntity?
    @Query("SELECT * FROM mascot_progress WHERE id = 1")
    fun observeMascot(): Flow<MascotEntity?>
    @Upsert suspend fun saveMascot(mascot: MascotEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun award(award: XpAwardEntity): Long
    @Query("SELECT * FROM xp_awards ORDER BY awardedAt DESC, eventKey DESC LIMIT 200")
    fun observeAwards(): Flow<List<XpAwardEntity>>
    @Query("SELECT * FROM habit_days WHERE localDate = :date")
    suspend fun day(date: String): HabitDayEntity?
    @Upsert suspend fun saveDay(day: HabitDayEntity)
    @Query("SELECT COUNT(*) FROM habit_completions")
    suspend fun completionCount(): Long
}
