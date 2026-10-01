package com.ansa1r.projectadhd.data.local

import com.ansa1r.projectadhd.data.local.dao.BlockSessionDao
import com.ansa1r.projectadhd.data.local.entity.BlockSessionEntity
import com.ansa1r.projectadhd.data.local.entity.*
import com.ansa1r.projectadhd.data.local.dao.HabitProgressDao
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
    entities = [HabitEntity::class, HabitCompletionEntity::class, TrackedAppEntity::class, InterventionEventEntity::class, BlockSessionEntity::class, HabitDailyEntity::class, MascotEntity::class, XpAwardEntity::class, HabitDayEntity::class],
    version = 3, exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progress(): HabitProgressDao
    abstract fun blocks(): BlockSessionDao
    abstract fun habits(): HabitDao
    abstract fun trackedApps(): TrackedAppDao
    abstract fun interventions(): InterventionDao
}
