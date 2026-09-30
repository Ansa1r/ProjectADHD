package com.ansa1r.projectadhd

import android.content.Context
import androidx.room.Room
import com.ansa1r.projectadhd.data.local.AppDatabase
import com.ansa1r.projectadhd.data.preferences.AppPreferences
import com.ansa1r.projectadhd.data.repository.HabitRepository
import com.ansa1r.projectadhd.data.repository.InterventionRepository
import com.ansa1r.projectadhd.data.repository.TrackedAppRepository
import com.ansa1r.projectadhd.domain.intervention.InterventionEngine
import com.ansa1r.projectadhd.domain.model.RecordCounts
import com.ansa1r.projectadhd.monitoring.InstalledAppReader
import com.ansa1r.projectadhd.monitoring.MonitoringController
import com.ansa1r.projectadhd.monitoring.MonitoringState
import com.ansa1r.projectadhd.monitoring.PermissionManager
import com.ansa1r.projectadhd.monitoring.UsageStatsReader
import com.ansa1r.projectadhd.notification.NotificationHelper
import kotlinx.coroutines.flow.combine

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "projectadhd.db").build()
    }
    val habits by lazy { HabitRepository(database.habits()) }
    val trackedApps by lazy { TrackedAppRepository(database.trackedApps()) }
    val interventions by lazy { InterventionRepository(database.interventions()) }
    val preferences = AppPreferences(appContext)
    val permissions = PermissionManager(appContext)
    val installedApps = InstalledAppReader(appContext)
    val usage = UsageStatsReader(appContext, permissions)
    val monitoring = MonitoringState()
    val controller = MonitoringController(appContext, permissions, monitoring)
    val notifications = NotificationHelper(appContext)
    val engine = InterventionEngine()
    fun recordCounts() = combine(
        database.habits().observeHabitCount(), database.habits().observeCompletionCount(),
        database.trackedApps().observeCount(), database.interventions().observeCount()
    ) { habits, completions, apps, events -> RecordCounts(habits, completions, apps, events) }
}
