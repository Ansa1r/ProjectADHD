package com.ansa1r.projectadhd.data.local

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
    entities = [HabitEntity::class, HabitCompletionEntity::class, TrackedAppEntity::class, InterventionEventEntity::class],
    version = 1, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habits(): HabitDao
    abstract fun trackedApps(): TrackedAppDao
    abstract fun interventions(): InterventionDao
}
