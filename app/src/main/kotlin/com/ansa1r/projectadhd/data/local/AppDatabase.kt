package com.ansa1r.projectadhd.data.local

import com.ansa1r.projectadhd.data.local.dao.BlockSessionDao
import com.ansa1r.projectadhd.data.local.entity.BlockSessionEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import com.ansa1r.projectadhd.data.local.dao.HabitDao
import com.ansa1r.projectadhd.data.local.dao.InterventionDao
import com.ansa1r.projectadhd.data.local.dao.TrackedAppDao
import com.ansa1r.projectadhd.data.local.entity.HabitCompletionEntity
import com.ansa1r.projectadhd.data.local.entity.HabitEntity
import com.ansa1r.projectadhd.data.local.entity.InterventionEventEntity
import com.ansa1r.projectadhd.data.local.entity.TrackedAppEntity

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class, TrackedAppEntity::class, InterventionEventEntity::class, BlockSessionEntity::class],
    version = 2, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blocks(): BlockSessionDao
    abstract fun habits(): HabitDao
    abstract fun trackedApps(): TrackedAppDao
    abstract fun interventions(): InterventionDao
}
