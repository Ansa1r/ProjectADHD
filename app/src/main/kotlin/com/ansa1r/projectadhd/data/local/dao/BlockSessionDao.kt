package com.ansa1r.projectadhd.data.local.dao

import androidx.room.*
import com.ansa1r.projectadhd.data.local.entity.BlockSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockSessionDao {
    @Query("SELECT * FROM block_sessions WHERE active = 1 ORDER BY startedAt")
    suspend fun active(): List<BlockSessionEntity>
    @Query("SELECT * FROM block_sessions WHERE active = 1 ORDER BY startedAt")
    fun observeActive(): Flow<List<BlockSessionEntity>>
    @Query("SELECT * FROM block_sessions WHERE packageName = :packageName")
    suspend fun find(packageName: String): BlockSessionEntity?
    @Query("SELECT MAX(occurredAt) FROM intervention_events WHERE type = 'BLOCK_RELEASED' AND detail = 'COMPLETION'")
    fun observeLastUnlock(): Flow<Long?>
    @Upsert
    suspend fun upsert(session: BlockSessionEntity)
    @Query("UPDATE block_sessions SET active = 0, releasedAt = :now, releaseReason = :reason WHERE packageName = :packageName AND active = 1")
    suspend fun release(packageName: String, now: Long, reason: String): Int
}
