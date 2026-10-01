package com.ansa1r.projectadhd

import android.content.Context
import com.ansa1r.projectadhd.ui.apps.AppIconLoader
import androidx.room.Room
import com.ansa1r.projectadhd.data.local.Migrations
import com.ansa1r.projectadhd.data.repository.BlockRepository
import com.ansa1r.projectadhd.monitoring.ExcludedApps
import com.ansa1r.projectadhd.overlay.OverlayController
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import com.ansa1r.projectadhd.domain.model.TrackedApp

class AppContainer(context: Context) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val uiEntries = com.ansa1r.projectadhd.domain.startup.ForegroundEntryTracker()
    private val appContext = context.applicationContext
    private val database by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "projectadhd.db").addMigrations(Migrations.MIGRATION_1_2).build()
    }
    val blocks by lazy { BlockRepository(database) }
    val habits by lazy { HabitRepository(database, blocks) }
    val trackedApps by lazy { TrackedAppRepository(database, blocks) }
    val interventions by lazy { InterventionRepository(database.interventions()) }
    val preferences = AppPreferences(appContext)
    val permissions = PermissionManager(appContext)
    val excludedApps = ExcludedApps(appContext)
    val overlays = OverlayController(appContext, permissions, excludedApps)
    init {
        applicationScope.launch {
            preferences.blockingOverlayOpacity
                .catch { overlays.recordError("OPACITY_READ: " + it.javaClass.simpleName) }
                .collect(overlays::setBlockingOpacity)
        }
    }
    val appIcons = AppIconLoader(appContext)
    val installedApps = InstalledAppReader(appContext, excludedApps)
    val usage = UsageStatsReader(appContext, permissions)
    val monitoring = MonitoringState()
    val controller by lazy { MonitoringController(appContext, permissions, monitoring, overlays, blocks) }
    val notifications = NotificationHelper(appContext)
    val engine = InterventionEngine()
    suspend fun saveTrackedSelection(apps: List<TrackedApp>) = controller.gate.withLock {
        require(apps.none { excludedApps.contains(it.packageName) })
        trackedApps.replaceSelection(apps)
        val packages = apps.map { it.packageName }.toSet()
        if (overlays.state.value.packageName?.let { it !in packages } == true) overlays.hide()
        if (packages.isEmpty()) overlays.cancelTest()
        monitoring.selectionSaved(apps)
    }
    fun recordCounts() = combine(
        database.habits().observeHabitCount(), database.habits().observeCompletionCount(),
        database.trackedApps().observeCount(), database.interventions().observeCount()
    ) { habits, completions, apps, events -> RecordCounts(habits, completions, apps, events) }
}
