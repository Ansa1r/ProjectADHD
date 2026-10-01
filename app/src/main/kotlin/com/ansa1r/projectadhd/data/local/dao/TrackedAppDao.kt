package com.ansa1r.projectadhd.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ansa1r.projectadhd.data.local.entity.TrackedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedAppDao {
    @Query("SELECT * FROM tracked_apps ORDER BY displayName COLLATE NOCASE, packageName")
    fun observeAll(): Flow<List<TrackedAppEntity>>

    @Query("SELECT * FROM tracked_apps WHERE packageName = :packageName")
    suspend fun find(packageName: String): TrackedAppEntity?

    @Upsert
    suspend fun save(app: TrackedAppEntity)

    @Query("DELETE FROM tracked_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    @Upsert
    suspend fun saveAll(apps: List<TrackedAppEntity>)

    @Query("DELETE FROM tracked_apps")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tracked_apps")
    fun observeCount(): Flow<Int>
}
