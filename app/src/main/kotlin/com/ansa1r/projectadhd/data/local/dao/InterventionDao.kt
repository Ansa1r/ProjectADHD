package com.ansa1r.projectadhd.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ansa1r.projectadhd.data.local.entity.InterventionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionDao {
    @Query("SELECT * FROM intervention_events ORDER BY occurredAt DESC, id DESC LIMIT 200")
    fun observeRecent(): Flow<List<InterventionEventEntity>>

    @Insert
    suspend fun insert(event: InterventionEventEntity)

    @Query("DELETE FROM intervention_events")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM intervention_events")
    fun observeCount(): Flow<Int>
}
